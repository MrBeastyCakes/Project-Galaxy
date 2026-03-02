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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CreateAsteroidUseCase @Inject constructor(
    private val repository: CelestialBodyRepository,
    private val physicsWorld: PhysicsWorld
) {
    suspend operator fun invoke(
        textContent: String = ""
    ): CelestialBody {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val mass = 0.2
        val radius = CelestialBody.computeRadius(mass)

        // Place asteroid in outer belt at random angle
        val beltRadius = 800.0
        val angle = (Math.random() * 2 * PI)
        val posX = cos(angle) * beltRadius
        val posY = sin(angle) * beltRadius
        val speed = 30.0
        val vx = -sin(angle) * speed
        val vy = cos(angle) * speed

        val body = CelestialBody(
            id = id,
            parentId = null,
            type = BodyType.ASTEROID,
            name = if (textContent.length > 20) textContent.take(20) + "…" else textContent,
            position = Vec2(posX, posY),
            velocity = Vec2(vx, vy),
            mass = mass,
            radius = radius,
            orbitRadius = beltRadius,
            orbitAngle = angle,
            isPinned = false,
            createdAt = now,
            lastAccessedAt = now,
            accessCount = 0,
            wordCount = textContent.split("\\s+".toRegex()).count { it.isNotEmpty() }
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
                bodyType = BodyType.ASTEROID,
                orbitRadius = beltRadius
            )
        )

        return body
    }
}
