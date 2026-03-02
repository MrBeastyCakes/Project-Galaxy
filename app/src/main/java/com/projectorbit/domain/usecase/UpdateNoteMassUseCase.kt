package com.projectorbit.domain.usecase

import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.domain.repository.CelestialBodyRepository
import javax.inject.Inject

/**
 * Recalculates and persists mass/radius for a celestial body based on
 * updated note metadata (word count, pin status, access count).
 *
 * Called whenever:
 *  - Note content is edited (word count changes)
 *  - Body is pinned/unpinned
 *  - Body is accessed (access count increments)
 *
 * Also updates the live PhysicsWorld so the mass change takes effect
 * immediately in the simulation without requiring a restart.
 */
class UpdateNoteMassUseCase @Inject constructor(
    private val repository: CelestialBodyRepository,
    private val physicsWorld: PhysicsWorld
) {
    suspend operator fun invoke(
        bodyId: String,
        wordCount: Int,
        accessCount: Int,
        isPinned: Boolean
    ) {
        val newMass = CelestialBody.computeMass(wordCount, accessCount, isPinned)
        val newRadius = CelestialBody.computeRadius(newMass)

        // Persist to Room
        repository.updateMassAndRadius(bodyId, newMass, newRadius)

        // Update live physics simulation
        physicsWorld.setMass(bodyId, newMass)
    }
}
