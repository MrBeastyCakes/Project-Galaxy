package com.projectorbit.data.mapper

import com.projectorbit.data.db.entity.BodyType as EntityBodyType
import com.projectorbit.data.db.entity.CelestialBodyEntity
import com.projectorbit.domain.model.BodyType as DomainBodyType
import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.util.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CelestialBodyMapperTest {

    private val now = System.currentTimeMillis()

    private fun makeEntity(
        id: String = "body-1",
        parentId: String? = null,
        type: EntityBodyType = EntityBodyType.SUN,
        posX: Double = 100.0,
        posY: Double = 200.0,
        velX: Double = 1.5,
        velY: Double = -0.5,
        mass: Double = 500.0,
        radius: Double = 30.0
    ) = CelestialBodyEntity(
        id = id,
        parentId = parentId,
        type = type,
        name = "Test Body",
        positionX = posX,
        positionY = posY,
        velocityX = velX,
        velocityY = velY,
        mass = mass,
        radius = radius,
        orbitRadius = 1000.0,
        orbitAngle = 1.57,
        isPinned = true,
        isShared = true,
        isCompleted = false,
        completedAt = null,
        createdAt = now,
        lastAccessedAt = now,
        accessCount = 5,
        wordCount = 200,
        isDeleted = false,
        deletedAt = null
    )

    @Test
    fun entityToDomain_mapsPositionToVec2() {
        val entity = makeEntity(posX = 123.456, posY = 789.012)
        val domain = entity.toDomain()

        assertEquals(123.456, domain.position.x, 1e-12)
        assertEquals(789.012, domain.position.y, 1e-12)
    }

    @Test
    fun entityToDomain_mapsVelocityToVec2() {
        val entity = makeEntity(velX = 3.14, velY = -2.71)
        val domain = entity.toDomain()

        assertEquals(3.14, domain.velocity.x, 1e-12)
        assertEquals(-2.71, domain.velocity.y, 1e-12)
    }

    @Test
    fun entityToDomain_mapsAllBodyTypes() {
        val mappings = mapOf(
            EntityBodyType.SUN to DomainBodyType.SUN,
            EntityBodyType.GAS_GIANT to DomainBodyType.GAS_GIANT,
            EntityBodyType.BINARY_STAR to DomainBodyType.BINARY_STAR,
            EntityBodyType.PLANET to DomainBodyType.PLANET,
            EntityBodyType.MOON to DomainBodyType.MOON,
            EntityBodyType.ASTEROID to DomainBodyType.ASTEROID,
            EntityBodyType.DWARF_PLANET to DomainBodyType.DWARF_PLANET,
            EntityBodyType.NEBULA to DomainBodyType.NEBULA
        )

        mappings.forEach { (entityType, domainType) ->
            val entity = makeEntity(type = entityType)
            val domain = entity.toDomain()
            assertEquals("Failed for $entityType", domainType, domain.type)
        }
    }

    @Test
    fun entityToDomain_preservesDoubleFields() {
        val entity = makeEntity(mass = 12345.6789, radius = 98.7654)
        val domain = entity.toDomain()

        assertEquals(12345.6789, domain.mass, 1e-9)
        assertEquals(98.7654, domain.radius, 1e-9)
    }

    @Test
    fun entityToDomain_nullParentIdPreserved() {
        val entity = makeEntity(parentId = null)
        val domain = entity.toDomain()
        assertNull(domain.parentId)
    }

    @Test
    fun entityToDomain_nonNullParentIdPreserved() {
        val entity = makeEntity(parentId = "parent-42")
        val domain = entity.toDomain()
        assertEquals("parent-42", domain.parentId)
    }

    @Test
    fun domainToEntity_mapsVec2ToSeparateFields() {
        val domain = CelestialBody(
            id = "body-2",
            parentId = null,
            type = DomainBodyType.PLANET,
            name = "Test Planet",
            position = Vec2(55.5, 66.6),
            velocity = Vec2(-1.1, 2.2),
            mass = 300.0,
            radius = 15.0,
            orbitRadius = 500.0,
            orbitAngle = 0.785,
            isPinned = false,
            isShared = false,
            isCompleted = false,
            completedAt = null,
            createdAt = now,
            lastAccessedAt = now,
            accessCount = 3,
            wordCount = 100,
            isDeleted = false,
            deletedAt = null
        )

        val entity = domain.toEntity()

        assertEquals(55.5, entity.positionX, 1e-12)
        assertEquals(66.6, entity.positionY, 1e-12)
        assertEquals(-1.1, entity.velocityX, 1e-12)
        assertEquals(2.2, entity.velocityY, 1e-12)
    }

    @Test
    fun roundTrip_entityToDomainToEntity() {
        val original = makeEntity(
            id = "rt-1",
            parentId = "parent-rt",
            type = EntityBodyType.MOON,
            posX = 42.0,
            posY = -17.5,
            velX = 0.3,
            velY = -0.7
        ).copy(isCompleted = true, completedAt = now)

        val domain = original.toDomain()
        val roundTripped = domain.toEntity()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.parentId, roundTripped.parentId)
        assertEquals(original.type, roundTripped.type)
        assertEquals(original.positionX, roundTripped.positionX, 1e-12)
        assertEquals(original.positionY, roundTripped.positionY, 1e-12)
        assertEquals(original.velocityX, roundTripped.velocityX, 1e-12)
        assertEquals(original.velocityY, roundTripped.velocityY, 1e-12)
        assertEquals(original.mass, roundTripped.mass, 1e-12)
        assertEquals(original.isCompleted, roundTripped.isCompleted)
        assertEquals(original.completedAt, roundTripped.completedAt)
        assertEquals(original.isShared, roundTripped.isShared)
    }
}
