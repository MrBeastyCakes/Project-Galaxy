package com.projectorbit.domain.physics

import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.model.PhysicsConstants
import kotlin.math.sqrt

/**
 * Computes gravitational accelerations for all bodies using a Barnes-Hut quadtree.
 *
 * Barnes-Hut reduces the O(n^2) naive gravity calculation to O(n log n) by treating
 * distant clusters of bodies as a single aggregate mass node.
 *
 * Moon bodies are excluded from the quadtree (they have negligible mass and do not
 * gravitationally influence other bodies per the spec).
 *
 * Gravity formula (softened): F = G * m1 * m2 / (r^2 + epsilon^2)
 * Acceleration on body1:       a = G * m2 / (r^2 + epsilon^2)  (direction toward body2)
 */
class GravityResolver(
    private val G: Double = PhysicsConstants.G,
    private val epsilon: Double = PhysicsConstants.EPSILON,
    private val theta: Double = PhysicsConstants.BARNES_HUT_THETA
) {

    // -------------------------------------------------------------------------
    // Barnes-Hut Quadtree
    // -------------------------------------------------------------------------

    private sealed class QuadNode {
        abstract val centerX: Double
        abstract val centerY: Double
        abstract val halfSize: Double
        abstract val totalMass: Double
        abstract val massCenterX: Double
        abstract val massCenterY: Double

        class Leaf(
            override val centerX: Double,
            override val centerY: Double,
            override val halfSize: Double,
            val body: PhysicsBody
        ) : QuadNode() {
            override val totalMass get() = body.mass
            override val massCenterX get() = body.positionX
            override val massCenterY get() = body.positionY
        }

        class Internal(
            override val centerX: Double,
            override val centerY: Double,
            override val halfSize: Double,
            override var totalMass: Double = 0.0,
            override var massCenterX: Double = 0.0,
            override var massCenterY: Double = 0.0,
            val children: Array<QuadNode?> = arrayOfNulls(4)
        ) : QuadNode()
    }

    /**
     * Compute gravitational acceleration for [body] from all other [bodies].
     * Returns (accelX, accelY).
     */
    fun computeAcceleration(body: PhysicsBody, bodies: List<PhysicsBody>): Pair<Double, Double> {
        if (bodies.size <= 1) return Pair(0.0, 0.0)

        val treeBodies = bodies.filter { it.bodyType != BodyType.MOON && it.id != body.id }
        if (treeBodies.isEmpty()) return Pair(0.0, 0.0)

        val root = buildTree(treeBodies) ?: return Pair(0.0, 0.0)
        return queryAcceleration(body, root)
    }

    /**
     * Compute gravitational accelerations for all [bodies] using the Barnes-Hut tree.
     * Sets each body's accelerationX/Y to the gravitational contribution (additive -- call
     * resetAcceleration() before this if you want gravity-only values).
     */
    fun resolveAll(bodies: List<PhysicsBody>) {
        val treeBodies = bodies.filter { it.bodyType != BodyType.MOON }
        if (treeBodies.isEmpty()) return

        val root = buildTree(treeBodies) ?: return

        for (body in bodies) {
            val (ax, ay) = queryAcceleration(body, root)
            body.addAcceleration(ax, ay)
        }
    }

    // -------------------------------------------------------------------------
    // Tree construction
    // -------------------------------------------------------------------------

    private fun buildTree(bodies: List<PhysicsBody>): QuadNode? {
        if (bodies.isEmpty()) return null

        // Compute bounding box
        var minX = bodies[0].positionX
        var maxX = bodies[0].positionX
        var minY = bodies[0].positionY
        var maxY = bodies[0].positionY
        for (b in bodies) {
            if (b.positionX < minX) minX = b.positionX
            if (b.positionX > maxX) maxX = b.positionX
            if (b.positionY < minY) minY = b.positionY
            if (b.positionY > maxY) maxY = b.positionY
        }
        val cx = (minX + maxX) * 0.5
        val cy = (minY + maxY) * 0.5
        val halfSize = maxOf((maxX - minX), (maxY - minY)) * 0.5 + 1.0

        val root = QuadNode.Internal(cx, cy, halfSize)
        for (b in bodies) {
            insertBody(root, b)
        }
        return root
    }

    private fun insertBody(node: QuadNode.Internal, body: PhysicsBody, depth: Int = 0) {
        // Update aggregate mass and center of mass
        val newMass = node.totalMass + body.mass
        node.massCenterX = (node.massCenterX * node.totalMass + body.positionX * body.mass) / newMass
        node.massCenterY = (node.massCenterY * node.totalMass + body.positionY * body.mass) / newMass
        node.totalMass = newMass

        // Guard against infinite recursion from coincident bodies
        if (depth > MAX_TREE_DEPTH) return

        val quadrant = quadrantFor(node, body.positionX, body.positionY)
        val existing = node.children[quadrant]

        when (existing) {
            null -> {
                node.children[quadrant] = QuadNode.Leaf(
                    centerX = childCenterX(node, quadrant),
                    centerY = childCenterY(node, quadrant),
                    halfSize = node.halfSize * 0.5,
                    body = body
                )
            }
            is QuadNode.Leaf -> {
                // Subdivide: promote leaf to internal, re-insert old body + new body
                val childNode = QuadNode.Internal(
                    centerX = childCenterX(node, quadrant),
                    centerY = childCenterY(node, quadrant),
                    halfSize = node.halfSize * 0.5
                )
                node.children[quadrant] = childNode
                insertBody(childNode, existing.body, depth + 1)
                insertBody(childNode, body, depth + 1)
            }
            is QuadNode.Internal -> {
                insertBody(existing, body, depth + 1)
            }
        }
    }

    companion object {
        /** Max quadtree depth to prevent stack overflow from coincident bodies. */
        private const val MAX_TREE_DEPTH = 40
    }

    private fun quadrantFor(node: QuadNode.Internal, x: Double, y: Double): Int {
        val right = x >= node.centerX
        val top = y >= node.centerY
        return when {
            right && top -> 0   // NE
            !right && top -> 1  // NW
            !right && !top -> 2 // SW
            else -> 3           // SE
        }
    }

    private fun childCenterX(node: QuadNode.Internal, quadrant: Int): Double {
        val q = node.halfSize * 0.5
        return if (quadrant == 0 || quadrant == 3) node.centerX + q else node.centerX - q
    }

    private fun childCenterY(node: QuadNode.Internal, quadrant: Int): Double {
        val q = node.halfSize * 0.5
        return if (quadrant == 0 || quadrant == 1) node.centerY + q else node.centerY - q
    }

    // -------------------------------------------------------------------------
    // Tree query
    // -------------------------------------------------------------------------

    private fun queryAcceleration(body: PhysicsBody, node: QuadNode): Pair<Double, Double> {
        return when (node) {
            is QuadNode.Leaf -> {
                if (node.body.id == body.id) Pair(0.0, 0.0)
                else gravitationalAccel(body, node.massCenterX, node.massCenterY, node.totalMass)
            }
            is QuadNode.Internal -> {
                val dx = node.massCenterX - body.positionX
                val dy = node.massCenterY - body.positionY
                val distSq = dx * dx + dy * dy
                val size = node.halfSize * 2.0

                // Barnes-Hut criterion: if (size / dist) < theta, treat as single mass
                if (size * size < theta * theta * distSq) {
                    gravitationalAccel(body, node.massCenterX, node.massCenterY, node.totalMass)
                } else {
                    var ax = 0.0
                    var ay = 0.0
                    for (child in node.children) {
                        if (child != null) {
                            val (cax, cay) = queryAcceleration(body, child)
                            ax += cax
                            ay += cay
                        }
                    }
                    Pair(ax, ay)
                }
            }
        }
    }

    /**
     * Compute gravitational acceleration on [body] from a mass at ([mx], [my]) with [mass].
     * Softened: a = G * mass / (r^2 + epsilon^2), directed toward the mass.
     */
    private fun gravitationalAccel(
        body: PhysicsBody,
        mx: Double,
        my: Double,
        mass: Double
    ): Pair<Double, Double> {
        val dx = mx - body.positionX
        val dy = my - body.positionY
        val distSq = dx * dx + dy * dy
        val softenedDistSq = distSq + epsilon * epsilon
        val forceMag = G * mass / softenedDistSq
        val dist = sqrt(softenedDistSq)
        return Pair(forceMag * dx / dist, forceMag * dy / dist)
    }

    /**
     * Naive O(n^2) gravity -- used for small body counts or unit testing.
     * Excludes Moon bodies from acting as attractors.
     */
    fun resolveNaive(bodies: List<PhysicsBody>) {
        for (i in bodies.indices) {
            val body = bodies[i]
            for (j in bodies.indices) {
                if (i == j) continue
                val other = bodies[j]
                if (other.bodyType == BodyType.MOON) continue
                val (ax, ay) = gravitationalAccel(body, other.positionX, other.positionY, other.mass)
                body.addAcceleration(ax, ay)
            }
        }
    }
}
