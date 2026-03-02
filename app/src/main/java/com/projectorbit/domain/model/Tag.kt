package com.projectorbit.domain.model

/**
 * Domain model for a tag that can be applied to any celestial body.
 * Tags control atmosphere color and density on the rendered body.
 */
data class Tag(
    val id: String,
    val name: String,
    /** ARGB packed int -- atmosphere glow color for this tag. */
    val atmosphereColor: Int,
    /** 0.0 (faint) to 1.0 (opaque) -- atmosphere density. */
    val atmosphereDensity: Double
)
