package com.projectorbit.domain.physics

import com.projectorbit.domain.model.BodyType
import kotlin.math.sqrt

/**
 * Two-phase collision detection:
 *  1. Broad phase: spatial hash grid (cell size = 2x the largest radius in the scene)
 *  2. Narrow phase: circle-circle intersection test for candidate pairs
 *
 * Collision resolution rules (per spec):
 *  - Asteroid + Planet  -> accretion event (merge)
 *  - Asteroid + Asteroid -> can form new Planet (merge)
 *  - Planet + Planet     -> repulsion (push apart, no merge)
 *  - Moon completion     -> dissolve event (handled separately via commands)
 */
class CollisionDetector {

    data class CollisionPair(val bodyA: PhysicsBody, val bodyB: PhysicsBody)

    sealed class CollisionEvent {
        /** Asteroid is absorbed into planet. */
        data class Accretion(val asteroidId: String, val planetId: String) : CollisionEvent()
        /** Two asteroids merge to potentially form a planet. */
        data class AsteroidMerge(val asteroidIdA: String, val asteroidIdB: String) : CollisionEvent()
        /** Two planets repel each other (no merge). */
        data class PlanetRepulsion(val planetIdA: String, val planetIdB: String) : CollisionEvent()
    }

    /**
     * Detect all colliding pairs among [bodies] using spatial hashing + circle test.
     * Returns a list of [CollisionEvent] to be processed by PhysicsWorld.
     */
    fun detect(bodies: List<PhysicsBody>): List<CollisionEvent> {
        if (bodies.size < 2) return emptyList()

        val maxRadius = bodies.maxOf { it.radius }
        val cellSize = maxOf(maxRadius * 2.0, 1.0)

        // Build spatial hash: cell key -> list of bodies in that cell
        val grid = HashMap<Long, MutableList<PhysicsBody>>()
        for (body in bodies) {
            for (cell in cellsFor(body, cellSize)) {
                grid.getOrPut(cell) { mutableListOf() }.add(body)
            }
        }

        // Collect candidate pairs (deduplicated)
        val checked = HashSet<Long>()
        val events = mutableListOf<CollisionEvent>()

        for ((_, cellBodies) in grid) {
            for (i in cellBodies.indices) {
                for (j in i + 1 until cellBodies.size) {
                    val a = cellBodies[i]
                    val b = cellBodies[j]
                    // Stable pair key (smaller id first)
                    val key = pairKey(a.id, b.id)
                    if (!checked.add(key)) continue

                    if (circlesOverlap(a, b)) {
                        resolveCollision(a, b)?.let { events.add(it) }
                    }
                }
            }
        }

        return events
    }

    // -------------------------------------------------------------------------
    // Spatial hash helpers
    // -------------------------------------------------------------------------

    /** Returns the set of grid cell keys that the body overlaps. */
    private fun cellsFor(body: PhysicsBody, cellSize: Double): List<Long> {
        val minCX = cellCoord(body.positionX - body.radius, cellSize)
        val maxCX = cellCoord(body.positionX + body.radius, cellSize)
        val minCY = cellCoord(body.positionY - body.radius, cellSize)
        val maxCY = cellCoord(body.positionY + body.radius, cellSize)

        val cells = mutableListOf<Long>()
        for (cx in minCX..maxCX) {
            for (cy in minCY..maxCY) {
                cells.add(encodeCellKey(cx, cy))
            }
        }
        return cells
    }

    private fun cellCoord(pos: Double, cellSize: Double): Int =
        kotlin.math.floor(pos / cellSize).toInt()

    private fun encodeCellKey(cx: Int, cy: Int): Long =
        (cx.toLong() and 0xFFFFFFFFL) or ((cy.toLong() and 0xFFFFFFFFL) shl 32)

    private fun pairKey(idA: String, idB: String): Long {
        val a = idA.hashCode().toLong()
        val b = idB.hashCode().toLong()
        return if (a < b) (a shl 32) or (b and 0xFFFFFFFFL)
        else (b shl 32) or (a and 0xFFFFFFFFL)
    }

    // -------------------------------------------------------------------------
    // Narrow phase
    // -------------------------------------------------------------------------

    private fun circlesOverlap(a: PhysicsBody, b: PhysicsBody): Boolean {
        val dx = a.positionX - b.positionX
        val dy = a.positionY - b.positionY
        val distSq = dx * dx + dy * dy
        val minDist = a.radius + b.radius
        return distSq < minDist * minDist
    }

    // -------------------------------------------------------------------------
    // Collision resolution
    // -------------------------------------------------------------------------

    private fun resolveCollision(a: PhysicsBody, b: PhysicsBody): CollisionEvent? {
        val typeA = a.bodyType
        val typeB = b.bodyType

        return when {
            // Asteroid + Planet (either order) -> accretion
            isAsteroid(typeA) && isPlanet(typeB) ->
                CollisionEvent.Accretion(asteroidId = a.id, planetId = b.id)
            isPlanet(typeA) && isAsteroid(typeB) ->
                CollisionEvent.Accretion(asteroidId = b.id, planetId = a.id)

            // Asteroid + Asteroid -> merge
            isAsteroid(typeA) && isAsteroid(typeB) ->
                CollisionEvent.AsteroidMerge(a.id, b.id)

            // Planet + Planet -> repulsion (apply push-apart impulse here)
            isPlanet(typeA) && isPlanet(typeB) -> {
                applyRepulsionImpulse(a, b)
                CollisionEvent.PlanetRepulsion(a.id, b.id)
            }

            // All other pairs (Sun-Sun, Moon-*, etc.) -> no event
            else -> null
        }
    }

    private fun isAsteroid(type: BodyType) = type == BodyType.ASTEROID
    private fun isPlanet(type: BodyType) =
        type == BodyType.PLANET || type == BodyType.DWARF_PLANET || type == BodyType.GAS_GIANT

    /**
     * Push two overlapping planets apart by applying equal and opposite impulses
     * proportional to the overlap depth and inverse masses.
     */
    private fun applyRepulsionImpulse(a: PhysicsBody, b: PhysicsBody) {
        val dx = a.positionX - b.positionX
        val dy = a.positionY - b.positionY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001)
        val overlap = (a.radius + b.radius) - dist
        if (overlap <= 0.0) return

        val nx = dx / dist
        val ny = dy / dist
        val impulse = overlap * 0.5

        // Push each body proportional to inverse mass
        val totalMass = a.mass + b.mass
        val factorA = b.mass / totalMass
        val factorB = a.mass / totalMass

        a.positionX += nx * impulse * factorA
        a.positionY += ny * impulse * factorA
        b.positionX -= nx * impulse * factorB
        b.positionY -= ny * impulse * factorB
    }
}
