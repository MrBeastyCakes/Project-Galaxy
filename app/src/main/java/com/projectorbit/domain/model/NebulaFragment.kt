package com.projectorbit.domain.model

import com.projectorbit.util.Vec2

/**
 * Domain model for a nebula fragment -- a searchable text remnant left
 * after a celestial body is deleted (supernova -> nebula).
 */
data class NebulaFragment(
    val id: String,
    val originalBodyId: String,
    val textFragment: String,
    val position: Vec2,
    val createdAt: Long,
    /** 1.0 = fresh/bright, 0.0 = fully faded and invisible. */
    val fadeFactor: Double
)
