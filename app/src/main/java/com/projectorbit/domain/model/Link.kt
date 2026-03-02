package com.projectorbit.domain.model

/**
 * Domain model for a directional link between two celestial bodies.
 * Used for constellation backlinks, tidal locks, and wormhole portals.
 */
data class Link(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val linkType: LinkType,
    val createdAt: Long
)
