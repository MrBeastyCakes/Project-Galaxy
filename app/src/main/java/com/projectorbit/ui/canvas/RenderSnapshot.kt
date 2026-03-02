package com.projectorbit.ui.canvas

import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.model.LinkType

/**
 * RenderSnapshot extends the physics snapshot with visual metadata needed by the renderer.
 *
 * The pure domain PhysicsSnapshot only carries physics state (position, velocity, mass, radius).
 * The renderer additionally needs name, color, atmosphere, tags, link data, and UI flags.
 *
 * GalaxyViewModel builds RenderSnapshot by merging PhysicsSnapshot with the latest
 * CelestialBody domain models from the repository.
 *
 * This keeps Android/visual concerns out of the pure domain layer.
 */
data class RenderBody(
    // Identity & physics (mirrors BodySnapshot fields)
    val id: String,
    val positionX: Double,
    val positionY: Double,
    val radius: Double,
    val mass: Double,
    val bodyType: BodyType,
    val parentId: String?,
    val orbitRadius: Double,
    val isFixed: Boolean,

    // Visual metadata
    val name: String,
    val color: Int,                  // ARGB packed int — base body color derived from type/tags
    val atmosphereColor: Int,        // ARGB packed int — 0 if no atmosphere
    val atmosphereDensity: Double,   // 0.0..1.0

    // UI state flags
    val isPinned: Boolean,           // Pinned bodies have boosted mass and gold highlight
    val isShared: Boolean,           // Renders ring system around planet
    val isCompleted: Boolean,        // Moon completion state
    val isSelected: Boolean = false  // Currently selected by user
)

data class RenderLink(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val linkType: LinkType
)

data class RenderSnapshot(
    val bodies: List<RenderBody>,
    val links: List<RenderLink>,
    val tickNumber: Long
) {
    companion object {
        val EMPTY = RenderSnapshot(emptyList(), emptyList(), 0L)
    }
}
