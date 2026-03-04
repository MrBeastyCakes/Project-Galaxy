package com.projectorbit.ui.canvas

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Camera manages world-space to screen-space transforms for the galaxy canvas.
 *
 * World coordinates are Double for precision on large canvases.
 * Screen coordinates are Float (sufficient for bounded screen dimensions).
 *
 * @Volatile fields allow the render thread to safely read camera state
 * written by the main thread without locking.
 */

/**
 * Atomic snapshot of the camera's animation target.
 * Bundled as a single @Volatile reference so reads/writes are never torn
 * across the render thread and the gesture/main thread.
 */
data class CameraTarget(val x: Double, val y: Double, val zoom: Float)

class Camera(
    val screenWidth: Float,
    val screenHeight: Float
) {

    // World-space focus point — written by update(), read by render thread
    @Volatile var centerX: Double = 0.0
    @Volatile var centerY: Double = 0.0

    // Zoom: 0.001 = galaxy view, 1.0 = solar system, 10.0 = planet, 100.0 = surface
    @Volatile var zoom: Float = 0.01f

    // Single atomic target — written by gesture/main thread, read by render thread
    @Volatile var target: CameraTarget = CameraTarget(0.0, 0.0, 0.01f)

    // Lerp speed: 0.1 = smooth, 1.0 = instant
    var smoothingFactor: Float = 0.12f

    // Zoom bounds
    val minZoom: Float = 0.001f
    val maxZoom: Float = 150.0f

    companion object {
        const val ZOOM_GALAXY_MIN = 0.001f
        const val ZOOM_GALAXY_MAX = 0.05f
        const val ZOOM_CLUSTER_MIN = 0.05f
        const val ZOOM_CLUSTER_MAX = 0.5f
        const val ZOOM_SYSTEM_MIN = 0.5f
        const val ZOOM_SYSTEM_MAX = 5.0f
        const val ZOOM_PLANET_MIN = 5.0f
        const val ZOOM_PLANET_MAX = 50.0f
        const val ZOOM_SURFACE_MIN = 50.0f
    }

    /**
     * Smoothly interpolate camera toward targets. Called once per render frame.
     * Reads target atomically once per frame to avoid tearing.
     */
    fun update(dt: Float) {
        // Frame-rate independent exponential smoothing:
        // sf is calibrated for 60fps; correct for actual dt so feel is consistent at any frame rate.
        val sf = 1f - (1f - smoothingFactor).pow(dt * 60f)
        val t = target  // single atomic read

        centerX += (t.x - centerX) * sf
        centerY += (t.y - centerY) * sf
        zoom += (t.zoom - zoom) * sf
    }

    /**
     * Convert world-space Double coordinates to screen-space Float pixel coordinates.
     */
    fun worldToScreen(worldX: Double, worldY: Double): Pair<Float, Float> {
        val cx = centerX
        val cy = centerY
        val z = zoom
        val sx = ((worldX - cx) * z + screenWidth / 2.0).toFloat()
        val sy = ((worldY - cy) * z + screenHeight / 2.0).toFloat()
        return Pair(sx, sy)
    }

    /**
     * Convert screen-space Float pixel coordinates to world-space Double.
     */
    fun screenToWorld(screenX: Float, screenY: Float): Pair<Double, Double> {
        val cx = centerX
        val cy = centerY
        val z = zoom.toDouble()
        val wx = (screenX - screenWidth / 2.0) / z + cx
        val wy = (screenY - screenHeight / 2.0) / z + cy
        return Pair(wx, wy)
    }

    /**
     * Animate camera to a world-space target with a given zoom level.
     */
    fun animateTo(targetX: Double, targetY: Double, targetZoomLevel: Float) {
        target = CameraTarget(targetX, targetY, targetZoomLevel.coerceIn(minZoom, maxZoom))
    }

    /**
     * Immediately snap camera with no animation.
     */
    fun snapTo(worldX: Double, worldY: Double, zoomLevel: Float) {
        val z = zoomLevel.coerceIn(minZoom, maxZoom)
        centerX = worldX
        centerY = worldY
        zoom = z
        target = CameraTarget(worldX, worldY, z)
    }

    /**
     * Pan camera by screen-space delta (gesture handler calls this).
     * Converts screen delta to world delta and updates targets.
     * Uses target.zoom (not smoothed zoom) so the conversion is consistent with
     * the zoom level being animated toward.
     */
    fun panBy(screenDx: Float, screenDy: Float) {
        val t = target
        val z = t.zoom.toDouble()
        target = t.copy(x = t.x - screenDx / z, y = t.y - screenDy / z)
    }

    /**
     * Apply zoom with a focal point in screen space (pinch-to-zoom).
     * Also incorporates centroid movement so a single call handles both
     * zoom-anchoring and two-finger pan — no separate panBy() needed.
     *
     * Uses the current smoothed centerX/Y (not target) for the focal world
     * computation so the anchor tracks what is visually under the fingers.
     */
    fun zoomBy(scaleFactor: Float, focalScreenX: Float, focalScreenY: Float) {
        // Use current smoothed state for focal world point so anchor matches visual
        val focalWorldX = centerX + (focalScreenX - screenWidth / 2.0) / zoom
        val focalWorldY = centerY + (focalScreenY - screenHeight / 2.0) / zoom
        val newZoom = (target.zoom * scaleFactor).coerceIn(minZoom, maxZoom)
        val z = newZoom.toDouble()
        // Place new center so that focalWorld maps to focalScreen at newZoom
        val newCenterX = focalWorldX - (focalScreenX - screenWidth / 2.0) / z
        val newCenterY = focalWorldY - (focalScreenY - screenHeight / 2.0) / z
        target = CameraTarget(newCenterX, newCenterY, newZoom)
    }

    /**
     * Returns the visible world rectangle for frustum culling.
     */
    fun getVisibleWorldRect(): RectF {
        val cx = centerX
        val cy = centerY
        val z = zoom.toDouble()
        val halfW = (screenWidth / 2.0) / z
        val halfH = (screenHeight / 2.0) / z
        return RectF(
            (cx - halfW).toFloat(),
            (cy - halfH).toFloat(),
            (cx + halfW).toFloat(),
            (cy + halfH).toFloat()
        )
    }

    /**
     * Returns true if the given world-space circle is at all visible on screen
     * (quick reject for rendering culling).
     */
    fun isVisible(worldX: Double, worldY: Double, worldRadius: Double): Boolean {
        val rect = getVisibleWorldRect()
        val margin = worldRadius.toFloat()
        return worldX.toFloat() >= rect.left - margin &&
            worldX.toFloat() <= rect.right + margin &&
            worldY.toFloat() >= rect.top - margin &&
            worldY.toFloat() <= rect.bottom + margin
    }

    /**
     * Returns the current LOD zoom level name for debugging.
     */
    fun currentLodName(): String = when {
        zoom < ZOOM_GALAXY_MAX -> "GALAXY"
        zoom < ZOOM_CLUSTER_MAX -> "CLUSTER"
        zoom < ZOOM_SYSTEM_MAX -> "SYSTEM"
        zoom < ZOOM_PLANET_MAX -> "PLANET"
        else -> "SURFACE"
    }

    /**
     * World radius in pixels for a given world-space radius.
     */
    fun worldRadiusToScreen(worldRadius: Double): Float = (worldRadius * zoom).toFloat()

    /**
     * Update screen dimensions (called on SurfaceView size changes).
     */
    fun onSurfaceSizeChanged(width: Float, height: Float): Camera =
        Camera(width, height).also {
            it.centerX = this.centerX
            it.centerY = this.centerY
            it.zoom = this.zoom
            it.target = this.target
            it.smoothingFactor = this.smoothingFactor
        }
}
