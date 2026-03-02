package com.projectorbit.domain.physics

import com.projectorbit.domain.model.PhysicsConstants
import kotlin.math.sqrt

/**
 * Velocity Verlet integration step.
 *
 * Velocity Verlet is a symplectic integrator that preserves energy better than
 * basic Euler and retains an explicit velocity for damping and velocity clamping.
 *
 * Integration order per tick:
 *  1. Half-step velocity using current acceleration
 *  2. Full-step position
 *  3. Recompute acceleration from new positions (done externally by GravityResolver/Constraints)
 *  4. Complete velocity step with new acceleration
 *  5. Apply damping
 *  6. Clamp velocity magnitude to maxVelocity
 *  7. NaN guard -- reset body if position becomes NaN
 */
object Integrator {

    /**
     * Apply the first half of the Velocity Verlet step:
     * half-step velocity + full-step position.
     * Call this BEFORE recomputing forces.
     */
    fun halfStepAndPosition(
        body: PhysicsBody,
        dt: Double = PhysicsConstants.FIXED_DT
    ) {
        // v(t + dt/2) = v(t) + 0.5 * a(t) * dt
        body.velocityX += 0.5 * body.accelerationX * dt
        body.velocityY += 0.5 * body.accelerationY * dt

        // x(t + dt) = x(t) + v(t + dt/2) * dt
        body.positionX += body.velocityX * dt
        body.positionY += body.velocityY * dt
    }

    /**
     * Apply the second half of the Velocity Verlet step:
     * complete velocity using the newly computed acceleration,
     * then apply damping and velocity clamp.
     * Call this AFTER recomputing forces for the new positions.
     *
     * @param newAccelX New acceleration X at updated position (forces/mass)
     * @param newAccelY New acceleration Y at updated position (forces/mass)
     * @param maxVelocity Hard cap on velocity magnitude
     * @param parentPosition Optional parent position for NaN reset
     */
    fun completeVelocity(
        body: PhysicsBody,
        newAccelX: Double,
        newAccelY: Double,
        dt: Double = PhysicsConstants.FIXED_DT,
        maxVelocity: Double = PhysicsConstants.MAX_VELOCITY,
        parentPositionX: Double? = null,
        parentPositionY: Double? = null
    ) {
        // v(t + dt) = v(t + dt/2) + 0.5 * a(t + dt) * dt
        body.velocityX += 0.5 * newAccelX * dt
        body.velocityY += 0.5 * newAccelY * dt

        // Apply damping
        body.velocityX *= body.damping
        body.velocityY *= body.damping

        // Clamp velocity magnitude
        val speedSq = body.velocityX * body.velocityX + body.velocityY * body.velocityY
        if (speedSq > maxVelocity * maxVelocity) {
            val scale = maxVelocity / sqrt(speedSq)
            body.velocityX *= scale
            body.velocityY *= scale
        }

        // Update stored acceleration for next half-step
        body.accelerationX = newAccelX
        body.accelerationY = newAccelY

        // NaN guard
        if (body.hasNaN()) {
            resetBodyToSafePosition(body, parentPositionX, parentPositionY)
        }
    }

    /**
     * Fully integrate a body in a single call (for simple/testing use cases).
     * The caller must supply the force function [forceAt] which computes
     * (accelX, accelY) for a given position.
     */
    fun integrate(
        body: PhysicsBody,
        dt: Double = PhysicsConstants.FIXED_DT,
        maxVelocity: Double = PhysicsConstants.MAX_VELOCITY,
        parentPositionX: Double? = null,
        parentPositionY: Double? = null,
        forceAt: (posX: Double, posY: Double) -> Pair<Double, Double>
    ) {
        // Step 1: half-step velocity + full-step position
        halfStepAndPosition(body, dt)

        // Step 2: recompute acceleration at new position
        val (newAccelX, newAccelY) = forceAt(body.positionX, body.positionY)

        // Step 3: complete velocity
        completeVelocity(body, newAccelX, newAccelY, dt, maxVelocity, parentPositionX, parentPositionY)
    }

    private fun resetBodyToSafePosition(
        body: PhysicsBody,
        parentX: Double?,
        parentY: Double?
    ) {
        val px = parentX ?: 0.0
        val py = parentY ?: 0.0
        // Place body at orbitRadius distance from parent along X axis
        body.positionX = px + body.orbitRadius
        body.positionY = py
        body.velocityX = 0.0
        body.velocityY = 0.0
        body.accelerationX = 0.0
        body.accelerationY = 0.0
    }
}
