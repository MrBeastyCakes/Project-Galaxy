package com.projectorbit.domain.physics

import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.model.PhysicsConstants
import com.projectorbit.util.Vec2
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Central physics world that owns all [PhysicsBody] instances and produces
 * [PhysicsSnapshot] objects for the renderer.
 *
 * Threading contract (from spec Section 4.6):
 *  - Physics thread: owns all PhysicsBody instances. Only this thread may mutate them.
 *    Each tick: drain command queue, accumulate forces, integrate, detect collisions,
 *    apply constraints, produce snapshot.
 *  - Main thread -> Physics: enqueue [PhysicsCommand] objects on [commandQueue].
 *  - Physics -> Renderer: [AtomicReference<PhysicsSnapshot>] swapped each tick.
 *  - No shared mutable state between threads.
 */
class PhysicsWorld(
    private val G: Double = PhysicsConstants.G,
    private val epsilon: Double = PhysicsConstants.EPSILON,
    private val maxVelocity: Double = PhysicsConstants.MAX_VELOCITY,
    private val dampingFactor: Double = PhysicsConstants.DAMPING_FACTOR,
    useBarnesHut: Boolean = true
) {

    // -------------------------------------------------------------------------
    // Body registry (physics-thread owned)
    // -------------------------------------------------------------------------

    private val bodies = mutableListOf<PhysicsBody>()
    private val bodyMap = HashMap<String, PhysicsBody>()

    // Binary star pairs: each entry is (idA, idB)
    private val binaryPairs = mutableSetOf<Pair<String, String>>()

    // Tidal lock pairs: each entry is (idA, idB) -- symmetric
    private val tidalLocks = mutableSetOf<Pair<String, String>>()

    // -------------------------------------------------------------------------
    // Thread-safe communication surfaces
    // -------------------------------------------------------------------------

    /** Main thread enqueues commands; physics thread drains at start of each tick. */
    val commandQueue: ConcurrentLinkedQueue<PhysicsCommand> = ConcurrentLinkedQueue()

    /** Renderer reads the latest snapshot via get(). Updated each tick via set(). */
    val snapshotRef: AtomicReference<PhysicsSnapshot> = AtomicReference(PhysicsSnapshot.EMPTY)

    // -------------------------------------------------------------------------
    // Sub-systems
    // -------------------------------------------------------------------------

    private val gravityResolver = GravityResolver(G, epsilon, PhysicsConstants.BARNES_HUT_THETA)
    private val collisionDetector = CollisionDetector()
    private val useBarnesHut = useBarnesHut

    // Collision events pending main-thread processing (e.g. database merges)
    private val pendingCollisionEvents = mutableListOf<CollisionDetector.CollisionEvent>()

    /** Snapshot of pending collision events for external consumers (e.g. ViewModel). */
    @Volatile
    var latestCollisionEvents: List<CollisionDetector.CollisionEvent> = emptyList()
        private set

    // -------------------------------------------------------------------------
    // Tick counter
    // -------------------------------------------------------------------------

    private var tickNumber = 0L

    // -------------------------------------------------------------------------
    // Main simulation tick -- called by PhysicsThread at 60Hz
    // -------------------------------------------------------------------------

    /**
     * Execute one physics tick with fixed timestep [dt].
     * Must only be called from the dedicated physics thread.
     */
    fun tick(dt: Double = PhysicsConstants.FIXED_DT) {
        // 1. Drain command queue
        drainCommands()

        if (bodies.isEmpty()) {
            snapshotRef.set(PhysicsSnapshot.EMPTY)
            return
        }

        // 2. Reset accelerations
        for (body in bodies) body.resetAcceleration()

        // 3. Accumulate gravity forces
        if (useBarnesHut) {
            gravityResolver.resolveAll(bodies)
        } else {
            gravityResolver.resolveNaive(bodies)
        }

        // 4. Apply binary star constraints (additional spring forces)
        for ((idA, idB) in binaryPairs) {
            val bA = bodyMap[idA] ?: continue
            val bB = bodyMap[idB] ?: continue
            Constraints.applyBinaryStarConstraint(bA, bB)
        }

        // 5. Velocity Verlet - first half-step (velocity + position)
        for (body in bodies) {
            if (body.isFixed) continue
            Integrator.halfStepAndPosition(body, dt)
        }

        // 6. Recompute accelerations at new positions (for second half of Verlet)
        for (body in bodies) body.resetAcceleration()
        if (useBarnesHut) {
            gravityResolver.resolveAll(bodies)
        } else {
            gravityResolver.resolveNaive(bodies)
        }

        // 7. Apply constraint forces (orbit rails, tidal locks)
        Constraints.applyAll(bodies, bodyMap, tidalLocks)

        // 8. Velocity Verlet - complete velocity step + damping + clamp + NaN guard
        for (body in bodies) {
            if (body.isFixed) {
                body.resetAcceleration()
                continue
            }
            val parent = body.parentId?.let { bodyMap[it] }
            Integrator.completeVelocity(
                body = body,
                newAccelX = body.accelerationX,
                newAccelY = body.accelerationY,
                dt = dt,
                maxVelocity = maxVelocity,
                parentPositionX = parent?.positionX,
                parentPositionY = parent?.positionY
            )
        }

        // 9. Detect collisions
        pendingCollisionEvents.clear()
        pendingCollisionEvents.addAll(collisionDetector.detect(bodies))
        latestCollisionEvents = pendingCollisionEvents.toList()

        // 10. Produce snapshot
        val snapshot = buildSnapshot()
        snapshotRef.set(snapshot)
        tickNumber++
    }

    // -------------------------------------------------------------------------
    // Command processing (physics thread only)
    // -------------------------------------------------------------------------

    private fun drainCommands() {
        while (true) {
            val cmd = commandQueue.poll() ?: break
            processCommand(cmd)
        }
    }

    private fun processCommand(cmd: PhysicsCommand) {
        when (cmd) {
            is PhysicsCommand.AddBody -> {
                if (!bodyMap.containsKey(cmd.body.id)) {
                    bodies.add(cmd.body)
                    bodyMap[cmd.body.id] = cmd.body
                }
            }
            is PhysicsCommand.RemoveBody -> {
                bodyMap.remove(cmd.bodyId)?.let { bodies.remove(it) }
                binaryPairs.removeAll { it.first == cmd.bodyId || it.second == cmd.bodyId }
                tidalLocks.removeAll { it.first == cmd.bodyId || it.second == cmd.bodyId }
            }
            is PhysicsCommand.MoveBody -> {
                bodyMap[cmd.bodyId]?.setPosition(cmd.position)
            }
            is PhysicsCommand.ApplyForce -> {
                val body = bodyMap[cmd.bodyId] ?: return
                body.velocityX += cmd.force.x / body.mass
                body.velocityY += cmd.force.y / body.mass
            }
            is PhysicsCommand.SetMass -> {
                bodyMap[cmd.bodyId]?.mass = cmd.mass
            }
            is PhysicsCommand.SetFixed -> {
                bodyMap[cmd.bodyId]?.isFixed = cmd.fixed
            }
            is PhysicsCommand.SetDamping -> {
                bodyMap[cmd.bodyId]?.damping = cmd.damping
            }
            is PhysicsCommand.UpdateOrbitRadius -> {
                bodyMap[cmd.bodyId]?.orbitRadius = cmd.orbitRadius
            }
            is PhysicsCommand.CreateTidalLock -> {
                val pair = canonicalPair(cmd.bodyIdA, cmd.bodyIdB)
                tidalLocks.add(pair)
            }
            is PhysicsCommand.RemoveTidalLock -> {
                val pair = canonicalPair(cmd.bodyIdA, cmd.bodyIdB)
                tidalLocks.remove(pair)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Snapshot production
    // -------------------------------------------------------------------------

    private fun buildSnapshot(): PhysicsSnapshot {
        val snapBodies = bodies.map { b ->
            BodySnapshot(
                id = b.id,
                positionX = b.positionX,
                positionY = b.positionY,
                radius = b.radius,
                mass = b.mass,
                bodyType = b.bodyType,
                parentId = b.parentId,
                isFixed = b.isFixed
            )
        }
        val checksum = PhysicsSnapshot.computeChecksum(snapBodies)
        return PhysicsSnapshot(snapBodies, tickNumber, checksum)
    }

    // -------------------------------------------------------------------------
    // Public API (main thread -- enqueues commands)
    // -------------------------------------------------------------------------

    fun enqueue(command: PhysicsCommand) {
        commandQueue.offer(command)
    }

    fun addBody(body: PhysicsBody) = enqueue(PhysicsCommand.AddBody(body))
    fun removeBody(id: String) = enqueue(PhysicsCommand.RemoveBody(id))
    fun moveBody(id: String, position: Vec2) = enqueue(PhysicsCommand.MoveBody(id, position))
    fun applyForce(id: String, force: Vec2) = enqueue(PhysicsCommand.ApplyForce(id, force))
    fun setMass(id: String, mass: Double) = enqueue(PhysicsCommand.SetMass(id, mass))
    fun setFixed(id: String, fixed: Boolean) = enqueue(PhysicsCommand.SetFixed(id, fixed))
    fun createTidalLock(idA: String, idB: String) = enqueue(PhysicsCommand.CreateTidalLock(idA, idB))
    fun removeTidalLock(idA: String, idB: String) = enqueue(PhysicsCommand.RemoveTidalLock(idA, idB))

    /**
     * Register a binary star pair. The pair is also treated as a single barycenter
     * mass node in the Barnes-Hut tree (handled by GravityResolver).
     */
    fun registerBinaryPair(idA: String, idB: String) {
        binaryPairs.add(canonicalPair(idA, idB))
    }

    /**
     * Read the current snapshot (safe for any thread).
     */
    fun getSnapshot(): PhysicsSnapshot = snapshotRef.get()

    /**
     * Serialize all body physics states for persistence (Room batch update).
     * Safe to call from any thread -- returns a snapshot-derived list.
     */
    fun getPhysicsStates(): List<BodyPhysicsState> {
        val snap = snapshotRef.get()
        return snap.bodies.map { b ->
            BodyPhysicsState(b.id, b.positionX, b.positionY, 0.0, 0.0)
        }
    }

    /** Returns body count (for testing/diagnostics). */
    fun bodyCount(): Int = bodies.size

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun canonicalPair(a: String, b: String): Pair<String, String> =
        if (a < b) Pair(a, b) else Pair(b, a)
}

/** DTO for persisting physics state to Room. */
data class BodyPhysicsState(
    val id: String,
    val x: Double,
    val y: Double,
    val vx: Double,
    val vy: Double
)
