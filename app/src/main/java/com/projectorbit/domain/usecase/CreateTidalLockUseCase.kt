package com.projectorbit.domain.usecase

import com.projectorbit.domain.model.Link
import com.projectorbit.domain.model.LinkType
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.LinkRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Creates a tidal lock between two celestial bodies.
 *
 * A tidal lock is a rigid distance constraint: moving one body drags the other,
 * maintaining their separation distance (plan Section 5.5).
 *
 * Persists a TIDAL_LOCK link in Room and registers the constraint in PhysicsWorld.
 */
class CreateTidalLockUseCase @Inject constructor(
    private val bodyRepository: CelestialBodyRepository,
    private val linkRepository: LinkRepository,
    private val physicsWorld: PhysicsWorld
) {
    suspend operator fun invoke(bodyIdA: String, bodyIdB: String): Link {
        val now = System.currentTimeMillis()

        val link = Link(
            id = UUID.randomUUID().toString(),
            sourceId = bodyIdA,
            targetId = bodyIdB,
            linkType = LinkType.TIDAL_LOCK,
            createdAt = now
        )

        linkRepository.upsert(link)
        physicsWorld.createTidalLock(bodyIdA, bodyIdB)

        return link
    }

    suspend fun remove(bodyIdA: String, bodyIdB: String) {
        linkRepository.deleteByEndpoints(bodyIdA, bodyIdB, LinkType.TIDAL_LOCK)
        linkRepository.deleteByEndpoints(bodyIdB, bodyIdA, LinkType.TIDAL_LOCK)
        physicsWorld.removeTidalLock(bodyIdA, bodyIdB)
    }
}
