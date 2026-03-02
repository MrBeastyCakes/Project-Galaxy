package com.projectorbit.domain.model

/**
 * Stellar evolution visual states for PLANET-type bodies.
 *
 * Note: these are VISUAL states applied by the renderer, not BodyType changes.
 * A body's BodyType remains PLANET throughout; only the rendering and mass change.
 * The BodyType.GAS_GIANT is used for hierarchy container nodes (sub-categories),
 * NOT for the gas-giant visual appearance of a high-word-count planet.
 *
 * State machine (per plan Section 7.2):
 *
 *   PROTOSTAR (new, <50 words) -> PLANET (has content)
 *   PLANET -> GAS_GIANT_VISUAL (wordCount > 1000)
 *   PLANET -> HIGH_MASS_VISUAL (pin/access high)
 *   PLANET -> DWARF_PLANET (no access > 60 days) -> PLANET (revived on access)
 *   PLANET -> SUPERNOVA (delete, 1.5s) -> NEBULA
 */
enum class StellarEvolutionState {
    /** New empty note -- pulsing warm glow, stabilizes once content > 50 words. */
    PROTOSTAR,

    /** Normal planet rendering -- solid color, atmosphere aura if tagged. */
    PLANET,

    /** Visual upgrade for high word-count notes (>1000 words). Banded gradient. */
    GAS_GIANT_VISUAL,

    /** Visual upgrade for highly-pinned/frequently accessed notes. Larger, tighter orbit. */
    HIGH_MASS_VISUAL,

    /** Decayed note -- not accessed in 60+ days. Desaturated, blue tint, outer orbit. */
    DWARF_PLANET,

    /** Deletion explosion animation -- 1.5s expanding particle ring. */
    SUPERNOVA,

    /** Post-deletion remnant -- translucent cloud, searchable text fragments. */
    NEBULA;

    companion object {
        /**
         * Derive the visual state from a [CelestialBody]'s current metadata.
         * Call this whenever note metadata changes to determine which render path to use.
         */
        fun from(
            type: BodyType,
            wordCount: Int,
            isPinned: Boolean,
            accessCount: Int,
            isDeleted: Boolean,
            lastAccessedAt: Long,
            nowMillis: Long = System.currentTimeMillis()
        ): StellarEvolutionState {
            if (isDeleted) return NEBULA
            if (type == BodyType.NEBULA) return NEBULA

            val ageMs = nowMillis - lastAccessedAt
            val ageDays = ageMs / 86_400_000L

            return when {
                type == BodyType.ASTEROID -> PLANET // asteroids use their own renderer
                type == BodyType.MOON -> PLANET
                type == BodyType.SUN -> PLANET
                type == BodyType.GAS_GIANT -> GAS_GIANT_VISUAL
                type == BodyType.BINARY_STAR -> GAS_GIANT_VISUAL
                type == BodyType.DWARF_PLANET -> DWARF_PLANET
                wordCount == 0 -> PROTOSTAR
                ageDays >= 60L -> DWARF_PLANET
                wordCount > 1000 -> GAS_GIANT_VISUAL
                isPinned || accessCount > 20 -> HIGH_MASS_VISUAL
                wordCount < 50 -> PROTOSTAR
                else -> PLANET
            }
        }
    }
}
