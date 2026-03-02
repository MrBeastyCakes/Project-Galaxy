package com.projectorbit.domain.usecase

import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.model.PhysicsConstants
import com.projectorbit.domain.physics.PhysicsBody
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.util.Vec2
import java.util.UUID
import javax.inject.Inject

class CreateSunUseCase @Inject constructor(
    private val repository: CelestialBodyRepository,
    private val physicsWorld: PhysicsWorld
) {
    suspend operator fun invoke(
        name: String,
        positionX: Double = 0.0,
        positionY: Double = 0.0
    ): CelestialBody {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val mass = 100.0
        val radius = CelestialBody.computeRadius(mass)

        val body = CelestialBody(
            id = id,
            parentId = null,
            type = BodyType.SUN,
            name = name,
            position = Vec2(positionX, positionY),
            velocity = Vec2(0.0, 0.0),
            mass = mass,
            radius = radius,
            orbitRadius = 0.0,
            orbitAngle = 0.0,
            isPinned = false,
            createdAt = now,
            lastAccessedAt = now,
            accessCount = 0,
            wordCount = 0
        )

        repository.upsert(body)

        physicsWorld.addBody(
            PhysicsBody(
                id = id,
                positionX = positionX,
                positionY = positionY,
                mass = mass,
                radius = radius,
                bodyType = BodyType.SUN,
                isFixed = true
            )
        )

        return body
    }
}
