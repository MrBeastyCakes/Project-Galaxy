package com.projectorbit.ui.canvas

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Camera manages world-space to screen-space transforms for the galaxy canvas.
 *
 * World coordinates are Double for precision on large canvases.
 * Screen coordinates are Float (sufficient for bounded screen dimensions).
 *
 * @Volatile fields allow the render thread to safely read camera state
 * written by the main thread without locking.
 */
class Camera(
    private val screenWidth: Float,
    private val screenHeight: Float
) {

    // World-space focus point — written by main/gesture thread, read by render thread
    @Volatile var centerX: Double = 0.0
    @Volatile var centerY: Double = 0.0

    // Zoom: 0.001 = galaxy view, 1.0 = solar system, 10.0 = planet, 100.0 = surface
    @Volatile var zoom: Float = 0.01f

    // Smooth animation targets — written by main thread
    @Volatile var targetCenterX: Double = 0.0
    @Volatile var targetCenterY: Double = 0.0
    @Volatile var targetZoom: Float = 0.01f

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
     * Copy-on-read: capture volatile fields into locals at frame start.
     */
    fun update(dt: Float) {
        val sf = smoothingFactor
        val cx = centerX
        val cy = centerY
        val cz = zoom
        val tx = targetCenterX
        val ty = targetCenterY
        val tz = targetZoom

        centerX = cx + (tx - cx) * sf
        centerY = cy + (ty - cy) * sf
        zoom = cz + (tz - cz) * sf
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
        targetCenterX = targetX
        targetCenterY = targetY
        targetZoom = targetZoomLevel.coerceIn(minZoom, maxZoom)
    }

    /**
     * Immediately snap camera with no animation.
     */
    fun snapTo(worldX: Double, worldY: Double, zoomLevel: Float) {
        centerX = worldX
        centerY = worldY
        zoom = zoomLevel.coerceIn(minZoom, maxZoom)
        targetCenterX = worldX
        targetCenterY = worldY
        targetZoom = zoom
    }

    /**
     * Pan camera by screen-space delta (gesture handler calls this).
     * Converts screen delta to world delta and updates targets.
     */
    fun panBy(screenDx: Float, screenDy: Float) {
        val z = zoom.toDouble()
        targetCenterX -= screenDx / z
        targetCenterY -= screenDy / z
    }

    /**
     * Apply zoom with a focal point in screen space (pinch-to-zoom).
     */
    fun zoomBy(scaleFactor: Float, focalScreenX: Float, focalScreenY: Float) {
        val (focalWorldX, focalWorldY) = screenToWorld(focalScreenX, focalScreenY)
        val newZoom = (targetZoom * scaleFactor).coerceIn(minZoom, maxZoom)
        // Adjust center so focal world point remains at the same screen position
        val z = newZoom.toDouble()
        val newCenterX = focalWorldX - (focalScreenX - screenWidth / 2.0) / z
        val newCenterY = focalWorldY - (focalScreenY - screenHeight / 2.0) / z
        targetZoom = newZoom
        targetCenterX = newCenterX
        targetCenterY = newCenterY
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
            it.targetCenterX = this.targetCenterX
            it.targetCenterY = this.targetCenterY
            it.targetZoom = this.targetZoom
            it.smoothingFactor = this.smoothingFactor
        }
}
