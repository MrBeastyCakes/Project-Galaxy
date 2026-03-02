package com.projectorbit.domain.physics

import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.model.PhysicsConstants
import com.projectorbit.util.Vec2

/**
 * Mutable physics state for a single simulated body.
 *
 * All coordinates use Double for world-space precision.
 * Only the physics thread mutates these fields after initial creation.
 * Other threads interact via [PhysicsCommand] objects enqueued on [PhysicsWorld].
 */
data class PhysicsBody(
    val id: String,
    var positionX: Double,
    var positionY: Double,
    var velocityX: Double = 0.0,
    var velocityY: Double = 0.0,
    /** Accumulated acceleration this tick (forces / mass). Reset each integration step. */
    var accelerationX: Double = 0.0,
    var accelerationY: Double = 0.0,
    var mass: Double,
    var radius: Double,
    val bodyType: BodyType,
    val parentId: String? = null,
    var orbitRadius: Double = 0.0,
    /** When true, position is locked (Suns don't drift). */
    var isFixed: Boolean = false,
    var damping: Double = PhysicsConstants.DAMPING_FACTOR
) {
    val position: Vec2 get() = Vec2(positionX, positionY)
    val velocity: Vec2 get() = Vec2(velocityX, velocityY)

    fun setPosition(v: Vec2) {
        positionX = v.x
        positionY = v.y
    }

    fun setVelocity(v: Vec2) {
        velocityX = v.x
        velocityY = v.y
    }

    fun addAcceleration(ax: Double, ay: Double) {
        accelerationX += ax
        accelerationY += ay
    }

    fun resetAcceleration() {
        accelerationX = 0.0
        accelerationY = 0.0
    }

    /** True if any physics field has become NaN (indicates runaway simulation). */
    fun hasNaN(): Boolean =
        positionX.isNaN() || positionY.isNaN() ||
        velocityX.isNaN() || velocityY.isNaN() ||
        accelerationX.isNaN() || accelerationY.isNaN()
}
