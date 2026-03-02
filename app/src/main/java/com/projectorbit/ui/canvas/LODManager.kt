package com.projectorbit.ui.canvas

import com.projectorbit.domain.model.BodyType

/**
 * LODManager determines what gets rendered at each zoom level.
 *
 * Uses fade bands (not hard thresholds) to smoothly transition visibility.
 * All decisions are based on the current Camera zoom value.
 */
class LODManager {

    companion object {
        // Fade band half-width (fraction of zoom range)
        private const val FADE_BAND = 0.005f

        // Zoom boundaries (match Camera constants)
        private const val ZOOM_GALAXY_MAX = Camera.ZOOM_GALAXY_MAX
        private const val ZOOM_CLUSTER_MAX = Camera.ZOOM_CLUSTER_MAX
        private const val ZOOM_SYSTEM_MAX = Camera.ZOOM_SYSTEM_MAX
        private const val ZOOM_PLANET_MAX = Camera.ZOOM_PLANET_MAX
        private const val ZOOM_SURFACE_MIN = Camera.ZOOM_SURFACE_MIN
    }

    enum class ZoomLevel {
        GALAXY, CLUSTER, SYSTEM, PLANET, SURFACE
    }

    /**
     * Represents a body that should be rendered at this frame, with alpha from LOD fade.
     */
    data class RenderEntry(
        val body: RenderBody,
        val alpha: Float // 0.0 = invisible, 1.0 = fully opaque
    )

    /**
     * Current zoom level (discrete).
     */
    fun currentZoomLevel(zoom: Float): ZoomLevel = when {
        zoom < ZOOM_GALAXY_MAX -> ZoomLevel.GALAXY
        zoom < ZOOM_CLUSTER_MAX -> ZoomLevel.CLUSTER
        zoom < ZOOM_SYSTEM_MAX -> ZoomLevel.SYSTEM
        zoom < ZOOM_PLANET_MAX -> ZoomLevel.PLANET
        else -> ZoomLevel.SURFACE
    }

    /**
     * Returns the set of bodies that should be rendered, sorted by BodyType ordinal
     * (back-to-front: Suns first, then Gas Giants, Planets, Moons, Asteroids, etc.)
     * with their LOD fade alpha values.
     *
     * Culling is performed using the visible world rect from the camera.
     */
    fun getVisibleBodies(
        snapshot: RenderSnapshot,
        camera: Camera
    ): List<RenderEntry> {
        val zoom = camera.zoom
        val level = currentZoomLevel(zoom)
        val rect = camera.getVisibleWorldRect()

        val result = mutableListOf<RenderEntry>()

        for (body in snapshot.bodies) {
            // Frustum cull — skip bodies fully outside viewport
            val screenR = camera.worldRadiusToScreen(body.radius).coerceAtLeast(8f)
            val bx = body.positionX.toFloat()
            val by = body.positionY.toFloat()
            if (bx + screenR < rect.left || bx - screenR > rect.right ||
                by + screenR < rect.top || by - screenR > rect.bottom
            ) {
                continue
            }

            val alpha = computeAlpha(body.bodyType, zoom, level)
            if (alpha > 0f) {
                result.add(RenderEntry(body, alpha))
            }
        }

        // Sort by BodyType ordinal for correct back-to-front draw order
        result.sortBy { it.body.bodyType.ordinal }
        return result
    }

    /**
     * Compute LOD fade alpha for a body type at a given zoom.
     * Uses smooth fade bands to avoid popping.
     */
    private fun computeAlpha(bodyType: BodyType, zoom: Float, level: ZoomLevel): Float {
        return when (bodyType) {
            BodyType.SUN -> {
                // Suns visible at all zoom levels
                1.0f
            }
            BodyType.GAS_GIANT, BodyType.BINARY_STAR -> {
                // Visible from cluster view onward
                fadeBand(zoom, ZOOM_GALAXY_MAX - FADE_BAND, ZOOM_GALAXY_MAX + FADE_BAND)
            }
            BodyType.PLANET, BodyType.DWARF_PLANET -> {
                // Visible from system view onward
                fadeBand(zoom, ZOOM_CLUSTER_MAX - FADE_BAND, ZOOM_CLUSTER_MAX + FADE_BAND)
            }
            BodyType.MOON -> {
                // Visible only in planet/surface view
                fadeBand(zoom, ZOOM_SYSTEM_MAX - FADE_BAND, ZOOM_SYSTEM_MAX + FADE_BAND)
            }
            BodyType.ASTEROID -> {
                // Asteroid belt visible at galaxy/cluster, hidden in close system view
                val appear = fadeBand(zoom, Camera.ZOOM_GALAXY_MIN, ZOOM_GALAXY_MAX + FADE_BAND)
                val disappear = 1f - fadeBand(zoom, ZOOM_CLUSTER_MAX - FADE_BAND, ZOOM_CLUSTER_MAX + FADE_BAND)
                appear * disappear.coerceAtLeast(0f)
            }
            BodyType.NEBULA -> {
                // Nebulae visible from cluster onward
                fadeBand(zoom, ZOOM_GALAXY_MAX - FADE_BAND, ZOOM_GALAXY_MAX + FADE_BAND)
            }
        }
    }

    /**
     * Returns 0.0 below fadeStart, 1.0 above fadeEnd, smooth linear in between.
     */
    private fun fadeBand(zoom: Float, fadeStart: Float, fadeEnd: Float): Float {
        return when {
            zoom <= fadeStart -> 0f
            zoom >= fadeEnd -> 1f
            else -> (zoom - fadeStart) / (fadeEnd - fadeStart)
        }
    }

    /**
     * Whether constellation lines should be drawn at the current zoom.
     * Visible in galaxy and cluster views; hidden up close.
     */
    fun shouldDrawConstellations(zoom: Float): Boolean =
        zoom < ZOOM_SYSTEM_MAX

    /**
     * Alpha for constellation lines (fades out as you zoom into a system).
     */
    fun constellationAlpha(zoom: Float): Float =
        1f - fadeBand(zoom, ZOOM_CLUSTER_MAX, ZOOM_SYSTEM_MAX)

    /**
     * Whether orbit path rings should be drawn at the current zoom.
     */
    fun shouldDrawOrbitPaths(zoom: Float): Boolean =
        zoom in ZOOM_CLUSTER_MAX..ZOOM_PLANET_MAX

    /**
     * Alpha for orbit paths.
     */
    fun orbitPathAlpha(zoom: Float): Float {
        val appear = fadeBand(zoom, ZOOM_CLUSTER_MAX - FADE_BAND, ZOOM_CLUSTER_MAX + FADE_BAND)
        val disappear = 1f - fadeBand(zoom, ZOOM_PLANET_MAX - 1f, ZOOM_PLANET_MAX)
        return (appear * disappear).coerceIn(0f, 1f) * 0.3f // always faint
    }

    /**
     * Whether body labels should be drawn.
     */
    fun shouldDrawLabels(zoom: Float): Boolean =
        zoom > ZOOM_CLUSTER_MAX

    /**
     * Alpha for body labels.
     */
    fun labelAlpha(zoom: Float): Float =
        fadeBand(zoom, ZOOM_CLUSTER_MAX, ZOOM_SYSTEM_MAX)

    /**
     * Whether to show the surface (text editor) transition.
     */
    fun isSurfaceLevel(zoom: Float): Boolean = zoom >= ZOOM_SURFACE_MIN
}
