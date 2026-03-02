package com.projectorbit.ui.gesture

import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsSnapshot
import com.projectorbit.ui.canvas.Camera
import kotlin.math.sqrt

/**
 * Maps screen-space touch coordinates to a [BodySnapshot] using the current
 * [PhysicsSnapshot] and [Camera] transform.
 *
 * Reads the snapshot directly (may be one frame stale — acceptable for tap/drag).
 * Accounts for zoom level: larger hit targets at galaxy zoom for tiny dots.
 */
class HitTester {

    /**
     * Returns the topmost celestial body at the given screen coordinates, or null.
     *
     * Bodies are tested in reverse draw order (front-to-back) so the
     * visually topmost body is picked first.
     */
    fun hitTest(
        screenX: Float,
        screenY: Float,
        snapshot: PhysicsSnapshot,
        camera: Camera
    ): BodySnapshot? {
        val (worldX, worldY) = camera.screenToWorld(screenX, screenY)
        val zoom = camera.zoom

        // Minimum touch radius in world units (scales with inverse zoom for usability)
        val minHitRadius = minHitRadiusWorld(zoom)

        // Test in reverse order (last drawn = topmost visually)
        for (body in snapshot.bodies.asReversed()) {
            val effectiveRadius = maxOf(body.radius, minHitRadius)
            val dx = worldX - body.positionX
            val dy = worldY - body.positionY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist <= effectiveRadius) {
                return body
            }
        }
        return null
    }

    /**
     * Minimum hit radius in world units, inversely scaled with zoom so tiny
     * galaxy-view dots remain tappable.
     */
    private fun minHitRadiusWorld(zoom: Float): Double {
        // 24dp equivalent at current zoom; higher at low zoom (galaxy view)
        return when {
            zoom < Camera.ZOOM_GALAXY_MAX -> 200.0   // galaxy: generous hit area
            zoom < Camera.ZOOM_CLUSTER_MAX -> 80.0
            zoom < Camera.ZOOM_SYSTEM_MAX -> 30.0
            zoom < Camera.ZOOM_PLANET_MAX -> 15.0
            else -> 8.0                               // surface: precise
        }
    }
}
