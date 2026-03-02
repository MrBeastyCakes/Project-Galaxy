package com.projectorbit.domain.physics

import com.projectorbit.domain.model.PhysicsConstants
import com.projectorbit.util.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Handles binary star pair physics (plan Section 4.7).
 *
 * Binary stars are two PhysicsBody instances orbiting their shared barycenter.
 * - Barycenter = mass-weighted midpoint of the two bodies
 * - Each body experiences a spring force toward its expected orbital position
 *   around the barycenter (similar to orbit rail but shorter, stiffer spring)
 * - For Barnes-Hut tree queries from other bodies, the pair is treated as a
 *   single aggregate node at the barycenter with combined mass.
 */
object BinaryStarSolver {

    /**
     * Place two binary star bodies into a figure-8-like mutual orbit.
     * bodyA and bodyB are placed symmetrically around the barycenter.
     *
     * @param bodyA First binary star body
     * @param bodyB Second binary star body
     * @param barycenterX World X of the shared barycenter
     * @param barycenterY World Y of the shared barycenter
     * @param separation Total separation distance between the two stars
     * @param initialAngle Starting angle of the orbit (radians)
     */
    fun placeInMutualOrbit(
        bodyA: PhysicsBody,
        bodyB: PhysicsBody,
        barycenterX: Double,
        barycenterY: Double,
        separation: Double = 60.0,
        initialAngle: Double = 0.0
    ) {
        val totalMass = bodyA.mass + bodyB.mass

        // Each body's orbit radius around the barycenter (lever arm rule)
        val rA = bodyB.mass / totalMass * separation
        val rB = bodyA.mass / totalMass * separation

        // Place bodyA
        bodyA.positionX = barycenterX + cos(initialAngle) * rA
        bodyA.positionY = barycenterY + sin(initialAngle) * rA

        // Place bodyB on the opposite side
        bodyB.positionX = barycenterX + cos(initialAngle + PI) * rB
        bodyB.positionY = barycenterY + sin(initialAngle + PI) * rB

        // Set orbital velocities (perpendicular to radial direction)
        val speedA = OrbitSolver.circularOrbitSpeed(bodyB.mass, rA)
        bodyA.velocityX = -sin(initialAngle) * speedA
        bodyA.velocityY = cos(initialAngle) * speedA

        val speedB = OrbitSolver.circularOrbitSpeed(bodyA.mass, rB)
        bodyB.velocityX = -sin(initialAngle + PI) * speedB
        bodyB.velocityY = cos(initialAngle + PI) * speedB

        // Store orbit radius on each body for constraint calculations
        bodyA.orbitRadius = rA
        bodyB.orbitRadius = rB
    }

    /**
     * Compute the barycenter position of a binary pair.
     */
    fun barycenter(bodyA: PhysicsBody, bodyB: PhysicsBody): Vec2 {
        val totalMass = bodyA.mass + bodyB.mass
        return Vec2(
            x = (bodyA.positionX * bodyA.mass + bodyB.positionX * bodyB.mass) / totalMass,
            y = (bodyA.positionY * bodyA.mass + bodyB.positionY * bodyB.mass) / totalMass
        )
    }

    /**
     * Combined mass of the binary pair (used as aggregate mass in Barnes-Hut).
     */
    fun combinedMass(bodyA: PhysicsBody, bodyB: PhysicsBody): Double =
        bodyA.mass + bodyB.mass

    /**
     * Apply spring constraints to keep each binary body near its orbit radius
     * around the barycenter. Stiffer spring than normal orbit rail.
     *
     * Call this during the force accumulation phase each tick.
     */
    fun applyBarycentricConstraint(
        bodyA: PhysicsBody,
        bodyB: PhysicsBody,
        springK: Double = PhysicsConstants.ORBIT_SPRING_K * 3.0
    ) {
        val bc = barycenter(bodyA, bodyB)

        fun applySpring(body: PhysicsBody) {
            val dx = body.positionX - bc.x
            val dy = body.positionY - bc.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001)
            val displacement = dist - body.orbitRadius
            val ax = -springK * displacement * dx / dist
            val ay = -springK * displacement * dy / dist
            body.addAcceleration(ax, ay)
        }

        applySpring(bodyA)
        applySpring(bodyB)
    }

    /**
     * Check whether two body IDs form a registered binary pair.
     */
    fun isPair(idA: String, idB: String, pairs: Set<Pair<String, String>>): Boolean {
        return pairs.contains(Pair(minOf(idA, idB), maxOf(idA, idB)))
    }
}
