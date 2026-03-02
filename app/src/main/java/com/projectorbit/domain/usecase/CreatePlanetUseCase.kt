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
import kotlin.math.cos
import kotlin.math.sin

class CreatePlanetUseCase @Inject constructor(
    private val repository: CelestialBodyRepository,
    private val physicsWorld: PhysicsWorld
) {
    suspend operator fun invoke(
        name: String,
        parentId: String,
        parentPositionX: Double,
        parentPositionY: Double,
        orbitRadius: Double = 150.0,
        orbitAngle: Double = 0.0
    ): CelestialBody {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val mass = PhysicsConstants.MASS_FLOOR
        val radius = CelestialBody.computeRadius(mass)

        val posX = parentPositionX + cos(orbitAngle) * orbitRadius
        val posY = parentPositionY + sin(orbitAngle) * orbitRadius

        // Tangential velocity for circular orbit
        val speed = Math.sqrt(PhysicsConstants.G * 100.0 / orbitRadius)
        val vx = -sin(orbitAngle) * speed
        val vy = cos(orbitAngle) * speed

        val body = CelestialBody(
            id = id,
            parentId = parentId,
            type = BodyType.PLANET,
            name = name,
            position = Vec2(posX, posY),
            velocity = Vec2(vx, vy),
            mass = mass,
            radius = radius,
            orbitRadius = orbitRadius,
            orbitAngle = orbitAngle,
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
                positionX = posX,
                positionY = posY,
                velocityX = vx,
                velocityY = vy,
                mass = mass,
                radius = radius,
                bodyType = BodyType.PLANET,
                parentId = parentId,
                orbitRadius = orbitRadius
            )
        )

        return body
    }
}
