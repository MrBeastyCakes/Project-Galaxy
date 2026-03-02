package com.projectorbit.domain.physics

import com.projectorbit.domain.model.PhysicsConstants
import com.projectorbit.util.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculates stable initial orbital parameters and handles orbital decay for bodies
 * that haven't been accessed recently.
 *
 * Orbital mechanics:
 *  - Circular orbit velocity: v = sqrt(G * M / r)  where M is parent mass, r is orbit radius
 *  - Bodies are placed at a given angle around their parent
 *  - Orbital decay: bodies not accessed for DECAY_THRESHOLD_DAYS drift outward
 */
object OrbitSolver {

    /**
     * Compute the stable circular orbit velocity magnitude for a body orbiting
     * a parent of [parentMass] at [orbitRadius].
     * v = sqrt(G * parentMass / orbitRadius)
     */
    fun circularOrbitSpeed(
        parentMass: Double,
        orbitRadius: Double,
        G: Double = PhysicsConstants.G
    ): Double {
        if (orbitRadius <= 0.0 || parentMass <= 0.0) return 0.0
        return sqrt(G * parentMass / orbitRadius) * PhysicsConstants.TANGENTIAL_VELOCITY_FACTOR
    }

    /**
     * Place [body] into a circular orbit around [parent] at [orbitAngle] radians.
     * Sets the body's position and velocity for a stable orbit.
     */
    fun placeInOrbit(
        body: PhysicsBody,
        parent: PhysicsBody,
        orbitAngle: Double = 0.0
    ) {
        val r = body.orbitRadius
        // Position: parent center + radial offset
        body.positionX = parent.positionX + cos(orbitAngle) * r
        body.positionY = parent.positionY + sin(orbitAngle) * r

        // Velocity: perpendicular to radial direction (counter-clockwise)
        val speed = circularOrbitSpeed(parent.mass, r)
        body.velocityX = -sin(orbitAngle) * speed
        body.velocityY = cos(orbitAngle) * speed
    }

    /**
     * Evenly distribute [count] bodies around a circle of [orbitRadius] centered at [parent].
     * Returns a list of (angleRad) values, one per body.
     */
    fun evenOrbitAngles(count: Int, offsetAngle: Double = 0.0): List<Double> {
        if (count <= 0) return emptyList()
        val step = 2.0 * PI / count
        return List(count) { i -> offsetAngle + i * step }
    }

    /**
     * Compute the outward drift displacement for a body undergoing orbital decay.
     * Decay begins after [PhysicsConstants.DECAY_THRESHOLD_DAYS] days without access.
     *
     * @param lastAccessedAt Epoch millis of last access
     * @param nowMillis Current epoch millis
     * @return Outward drift delta to add to orbitRadius this tick
     */
    fun decayDrift(
        lastAccessedAt: Long,
        nowMillis: Long,
        decayDriftRate: Double = PhysicsConstants.DECAY_DRIFT_RATE
    ): Double {
        val ageMs = nowMillis - lastAccessedAt
        val ageDays = ageMs / PhysicsConstants.MS_PER_DAY.toDouble()
        if (ageDays < PhysicsConstants.DECAY_THRESHOLD_DAYS) return 0.0
        val excessDays = ageDays - PhysicsConstants.DECAY_THRESHOLD_DAYS
        return decayDriftRate * excessDays * PhysicsConstants.FIXED_DT
    }

    /**
     * Apply outward drift to a body relative to its parent.
     * Moves the body radially away from its parent by [driftAmount].
     */
    fun applyDecayDrift(body: PhysicsBody, parent: PhysicsBody, driftAmount: Double) {
        if (driftAmount <= 0.0) return
        val dx = body.positionX - parent.positionX
        val dy = body.positionY - parent.positionY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001)
        val nx = dx / dist
        val ny = dy / dist
        body.positionX += nx * driftAmount
        body.positionY += ny * driftAmount
        body.orbitRadius += driftAmount
    }

    /**
     * Compute the suggested orbit radius for a new child body based on how many
     * siblings already exist and their orbit radii.
     * Returns a radius that doesn't overlap with existing siblings.
     */
    fun suggestOrbitRadius(
        parentRadius: Double,
        existingSiblingRadii: List<Double>,
        baseSpacing: Double = 30.0
    ): Double {
        val maxExisting = existingSiblingRadii.maxOrNull() ?: 0.0
        return maxOf(parentRadius + baseSpacing, maxExisting + baseSpacing)
    }
}
