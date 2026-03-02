package com.projectorbit.ui.canvas

import android.graphics.Color
import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.model.Link
import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsSnapshot
import com.projectorbit.util.blendAtmospheres

/**
 * Merges a [PhysicsSnapshot] (positions, radii, mass) with [CelestialBody] domain models
 * (name, tags, visual flags) into a [RenderSnapshot] for the renderer.
 *
 * Called by GalaxyViewModel on each physics tick before posting to GalaxySurfaceView.
 * Pure function — no side effects, no Android dependencies beyond android.graphics.Color.
 */
object RenderSnapshotBuilder {

    /**
     * Build a [RenderSnapshot] from the latest physics snapshot and domain body map.
     *
     * @param physicsSnapshot  Latest immutable snapshot from PhysicsWorld
     * @param bodyMap          Map of body ID -> CelestialBody (latest from repository)
     * @param links            Latest links from repository
     * @param selectedBodyId   Currently selected body ID (adds isSelected flag)
     */
    fun build(
        physicsSnapshot: PhysicsSnapshot,
        bodyMap: Map<String, CelestialBody>,
        links: List<Link>,
        selectedBodyId: String? = null
    ): RenderSnapshot {
        val renderBodies = physicsSnapshot.bodies.map { snap ->
            val domain = bodyMap[snap.id]
            buildRenderBody(snap, domain, snap.id == selectedBodyId)
        }

        val renderLinks = links.map { link ->
            RenderLink(
                id = link.id,
                sourceId = link.sourceId,
                targetId = link.targetId,
                linkType = link.linkType
            )
        }

        return RenderSnapshot(
            bodies = renderBodies,
            links = renderLinks,
            tickNumber = physicsSnapshot.tickNumber
        )
    }

    private fun buildRenderBody(
        snap: BodySnapshot,
        domain: CelestialBody?,
        isSelected: Boolean
    ): RenderBody {
        val name = domain?.name ?: ""
        val bodyColor = defaultBodyColor(snap.bodyType)

        // Atmosphere: blend all tag atmospheres additively
        val atmospheres = domain?.tags?.map { tag ->
            Pair(tag.atmosphereColor, tag.atmosphereDensity)
        } ?: emptyList()

        val atmosphereColor = if (atmospheres.isNotEmpty()) blendAtmospheres(atmospheres) else 0
        val atmosphereDensity = domain?.tags?.maxOfOrNull { it.atmosphereDensity } ?: 0.0

        return RenderBody(
            id = snap.id,
            positionX = snap.positionX,
            positionY = snap.positionY,
            radius = snap.radius,
            mass = snap.mass,
            bodyType = snap.bodyType,
            parentId = snap.parentId,
            orbitRadius = domain?.orbitRadius ?: 0.0,
            isFixed = snap.isFixed,
            name = name,
            color = bodyColor,
            atmosphereColor = atmosphereColor,
            atmosphereDensity = atmosphereDensity,
            isPinned = domain?.isPinned ?: false,
            isShared = domain?.isShared ?: false,
            isCompleted = domain?.isCompleted ?: false,
            isSelected = isSelected
        )
    }

    /**
     * Default base color for each body type.
     * These are ARGB packed ints (fully opaque).
     */
    private fun defaultBodyColor(bodyType: BodyType): Int = when (bodyType) {
        BodyType.SUN -> Color.rgb(255, 220, 80)
        BodyType.GAS_GIANT -> Color.rgb(180, 120, 60)
        BodyType.BINARY_STAR -> Color.rgb(220, 200, 255)
        BodyType.PLANET -> Color.rgb(80, 140, 200)
        BodyType.MOON -> Color.rgb(180, 180, 190)
        BodyType.ASTEROID -> Color.rgb(140, 130, 120)
        BodyType.DWARF_PLANET -> Color.rgb(120, 140, 160)
        BodyType.NEBULA -> Color.rgb(120, 80, 180)
    }
}
