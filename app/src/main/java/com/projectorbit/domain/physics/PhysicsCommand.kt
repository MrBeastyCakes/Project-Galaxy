package com.projectorbit.domain.physics

import com.projectorbit.util.Vec2

/**
 * Commands enqueued by the main thread and drained by the physics thread
 * at the start of each tick. This is the only safe way for external code
 * to mutate physics state -- no direct field access from outside the physics thread.
 */
sealed class PhysicsCommand {
    data class AddBody(val body: PhysicsBody) : PhysicsCommand()
    data class RemoveBody(val bodyId: String) : PhysicsCommand()
    data class MoveBody(val bodyId: String, val position: Vec2) : PhysicsCommand()
    data class ApplyForce(val bodyId: String, val force: Vec2) : PhysicsCommand()
    data class SetMass(val bodyId: String, val mass: Double) : PhysicsCommand()
    data class SetFixed(val bodyId: String, val fixed: Boolean) : PhysicsCommand()
    data class CreateTidalLock(val bodyIdA: String, val bodyIdB: String) : PhysicsCommand()
    data class RemoveTidalLock(val bodyIdA: String, val bodyIdB: String) : PhysicsCommand()
    data class SetDamping(val bodyId: String, val damping: Double) : PhysicsCommand()
    data class UpdateOrbitRadius(val bodyId: String, val orbitRadius: Double) : PhysicsCommand()
}
