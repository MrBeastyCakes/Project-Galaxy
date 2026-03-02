package com.projectorbit.domain.usecase

import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.physics.PhysicsWorld
import javax.inject.Inject

/**
 * Toggles a Moon (sub-task) between completed and incomplete states.
 *
 * On completion:
 *  - Sets isCompleted = true and completedAt timestamp in Room
 *  - Triggers dissolve animation (handled by the renderer via snapshot state)
 *  - Removes the moon from PhysicsWorld after animation completes (via RemoveBody command)
 *
 * On uncomplete: not supported post-dissolve -- completion is permanent per spec.
 * (Moon entity remains in Room with isCompleted=true for data integrity.)
 */
class ToggleMoonUseCase @Inject constructor(
    private val repository: CelestialBodyRepository,
    private val physicsWorld: PhysicsWorld
) {
    /**
     * Complete a moon sub-task.
     * @param moonId The body ID of the Moon entity to complete.
     * @param removeFromPhysicsAfterMs Delay in ms before removing from physics (for dissolve animation).
     */
    suspend operator fun invoke(moonId: String, removeFromPhysicsAfterMs: Long = 1000L) {
        val now = System.currentTimeMillis()
        repository.completeMoon(moonId, now)
        // Physics removal is scheduled by the ViewModel after the dissolve animation plays
        // The moon's isCompleted state in the snapshot signals the renderer to play dissolve
    }

    /**
     * Remove the moon's physics body after the dissolve animation completes.
     * Call this from the ViewModel after the animation duration has elapsed.
     */
    fun removePhysicsBody(moonId: String) {
        physicsWorld.removeBody(moonId)
    }
}
