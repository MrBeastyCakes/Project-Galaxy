package com.projectorbit.domain.usecase

import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.NebulaFragmentRepository
import com.projectorbit.domain.model.NebulaFragment
import com.projectorbit.util.Vec2
import java.util.UUID
import javax.inject.Inject

class DeleteBodyUseCase @Inject constructor(
    private val repository: CelestialBodyRepository,
    private val nebulaFragmentRepository: NebulaFragmentRepository,
    private val physicsWorld: PhysicsWorld
) {
    suspend operator fun invoke(bodyId: String, plainText: String, posX: Double, posY: Double) {
        val now = System.currentTimeMillis()

        // Soft-delete in Room
        repository.softDelete(bodyId, now)

        // Remove from physics simulation
        physicsWorld.removeBody(bodyId)

        // Create nebula fragment for searchable remnant
        if (plainText.isNotBlank()) {
            nebulaFragmentRepository.insert(
                NebulaFragment(
                    id = UUID.randomUUID().toString(),
                    originalBodyId = bodyId,
                    textFragment = plainText.take(500),
                    position = Vec2(posX, posY),
                    createdAt = now,
                    fadeFactor = 1.0
                )
            )
        }
    }
}
