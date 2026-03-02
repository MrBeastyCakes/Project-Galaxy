package com.projectorbit.domain.model

import com.projectorbit.util.Vec2

/**
 * Domain model for a celestial body. All positions/velocities use Double for
 * world-space precision. This is pure Kotlin with no Android dependencies.
 */
data class CelestialBody(
    val id: String,
    val parentId: String?,
    val type: BodyType,
    val name: String,
    val position: Vec2,
    val velocity: Vec2,
    val mass: Double,
    val radius: Double,
    val orbitRadius: Double,
    val orbitAngle: Double,
    val isPinned: Boolean,
    val isShared: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val accessCount: Int,
    val wordCount: Int,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val tags: List<Tag> = emptyList()
) {
    /** Compute mass from note metadata according to tuning parameters. */
    companion object {
        fun computeMass(
            wordCount: Int,
            accessCount: Int,
            isPinned: Boolean,
            massFloor: Double = PhysicsConstants.MASS_FLOOR,
            massWordCountFactor: Double = PhysicsConstants.MASS_WORD_COUNT_FACTOR,
            massAccessFactor: Double = PhysicsConstants.MASS_ACCESS_FACTOR,
            massPinBonus: Double = PhysicsConstants.MASS_PIN_BONUS
        ): Double {
            val base = massFloor +
                wordCount * massWordCountFactor +
                accessCount * massAccessFactor
            return if (isPinned) base + massPinBonus else base
        }

        fun computeRadius(mass: Double): Double = 5.0 + kotlin.math.sqrt(mass) * 1.5
    }
}
