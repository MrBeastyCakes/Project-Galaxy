package com.projectorbit.domain.physics

import com.projectorbit.domain.model.PhysicsConstants
import kotlin.math.sqrt

/**
 * Applies constraint forces each physics tick:
 *  1. Orbit rail -- soft spring keeping bodies near their orbitRadius from parent
 *  2. Tidal locking -- rigid-body-like distance constraint between two bodies
 *  3. Fixed body reset -- Suns and other fixed bodies are pinned to their stored position
 *  4. Tangential velocity injection -- maintains orbital motion direction
 */
object Constraints {

    /**
     * Apply all constraints to [bodies].
     *
     * @param bodies All active physics bodies
     * @param bodyMap Fast lookup map by id (pre-built by PhysicsWorld)
     * @param tidalLocks Set of body ID pairs that are tidal-locked
     */
    fun applyAll(
        bodies: List<PhysicsBody>,
        bodyMap: Map<String, PhysicsBody>,
        tidalLocks: Set<Pair<String, String>>,
        orbitSpringK: Double = PhysicsConstants.ORBIT_SPRING_K
    ) {
        for (body in bodies) {
            if (body.isFixed) {
                // Fixed bodies (Suns) stay locked -- velocity and acceleration zeroed
                body.velocityX = 0.0
                body.velocityY = 0.0
                body.accelerationX = 0.0
                body.accelerationY = 0.0
                continue
            }

            val parent = body.parentId?.let { bodyMap[it] }
            if (parent != null && body.orbitRadius > 0.0) {
                applyOrbitRail(body, parent, orbitSpringK)
                injectTangentialVelocity(body, parent)
            }
        }

        // Tidal lock constraints
        for ((idA, idB) in tidalLocks) {
            val bodyA = bodyMap[idA] ?: continue
            val bodyB = bodyMap[idB] ?: continue
            applyTidalLock(bodyA, bodyB)
        }
    }

    /**
     * Orbit rail: apply a spring force toward the target orbit radius.
     * F_spring = -k * (r - orbitRadius) * radial_direction
     */
    private fun applyOrbitRail(
        body: PhysicsBody,
        parent: PhysicsBody,
        springK: Double
    ) {
        val dx = body.positionX - parent.positionX
        val dy = body.positionY - parent.positionY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001)
        val displacement = dist - body.orbitRadius

        // Spring acceleration toward target radius (negative = toward parent, positive = away)
        val springAccel = -springK * displacement / dist
        body.addAcceleration(springAccel * dx, springAccel * dy)
    }

    /**
     * Inject tangential (perpendicular) velocity to maintain orbital motion.
     * Computes the ideal circular orbit speed and gently nudges velocity toward it.
     */
    private fun injectTangentialVelocity(body: PhysicsBody, parent: PhysicsBody) {
        val dx = body.positionX - parent.positionX
        val dy = body.positionY - parent.positionY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001)

        // Tangential direction (perpendicular to radial, counter-clockwise)
        val tx = -dy / dist
        val ty = dx / dist

        // Ideal circular orbit speed
        val idealSpeed = OrbitSolver.circularOrbitSpeed(parent.mass, dist)

        // Current tangential speed (projection of velocity onto tangential direction)
        val currentTangentialSpeed = body.velocityX * tx + body.velocityY * ty

        // Gentle nudge -- add a fraction of the difference each tick
        val correction = (idealSpeed - currentTangentialSpeed) * PhysicsConstants.TANGENTIAL_VELOCITY_FACTOR * 0.01
        body.velocityX += tx * correction
        body.velocityY += ty * correction
    }

    /**
     * Tidal lock: maintain a fixed distance between two bodies (rigid constraint).
     * Uses position-based correction (XPBD-style) -- moves bodies to satisfy the
     * distance constraint and adjusts velocities to prevent re-penetration.
     */
    private fun applyTidalLock(bodyA: PhysicsBody, bodyB: PhysicsBody) {
        val dx = bodyB.positionX - bodyA.positionX
        val dy = bodyB.positionY - bodyA.positionY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001)

        // Target distance is the average of both orbitRadii (used as the lock distance)
        val targetDist = (bodyA.orbitRadius + bodyB.orbitRadius) * 0.5
        val error = dist - targetDist
        if (kotlin.math.abs(error) < 0.01) return

        val nx = dx / dist
        val ny = dy / dist

        val totalMass = bodyA.mass + bodyB.mass
        val factorA = bodyB.mass / totalMass
        val factorB = bodyA.mass / totalMass

        // Positional correction (stiff -- apply 80% correction per tick)
        val correctionScale = error * 0.8
        bodyA.positionX += nx * correctionScale * factorA
        bodyA.positionY += ny * correctionScale * factorA
        bodyB.positionX -= nx * correctionScale * factorB
        bodyB.positionY -= ny * correctionScale * factorB

        // Velocity correction -- remove relative velocity along constraint axis
        val relVx = bodyB.velocityX - bodyA.velocityX
        val relVy = bodyB.velocityY - bodyA.velocityY
        val relVN = relVx * nx + relVy * ny
        bodyA.velocityX += nx * relVN * factorA
        bodyA.velocityY += ny * relVN * factorA
        bodyB.velocityX -= nx * relVN * factorB
        bodyB.velocityY -= ny * relVN * factorB
    }

    /**
     * Apply binary star barycenter constraint.
     * Each body of the binary pair orbits their shared barycenter.
     * Treats them as a spring-coupled pair and nudges toward their expected positions.
     */
    fun applyBinaryStarConstraint(bodyA: PhysicsBody, bodyB: PhysicsBody) {
        val totalMass = bodyA.mass + bodyB.mass

        // Barycenter
        val bcx = (bodyA.positionX * bodyA.mass + bodyB.positionX * bodyB.mass) / totalMass
        val bcy = (bodyA.positionY * bodyA.mass + bodyB.positionY * bodyB.mass) / totalMass

        // Each body's expected orbit radius around the barycenter (lever arm)
        val rA = bodyB.mass / totalMass * bodyA.orbitRadius
        val rB = bodyA.mass / totalMass * bodyA.orbitRadius

        fun applySpring(body: PhysicsBody, targetRadius: Double) {
            val dx = body.positionX - bcx
            val dy = body.positionY - bcy
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001)
            val disp = dist - targetRadius
            val springK = PhysicsConstants.ORBIT_SPRING_K * 2.0
            val ax = -springK * disp * dx / dist
            val ay = -springK * disp * dy / dist
            body.addAcceleration(ax, ay)
        }

        applySpring(bodyA, rA)
        applySpring(bodyB, rB)
    }
}
