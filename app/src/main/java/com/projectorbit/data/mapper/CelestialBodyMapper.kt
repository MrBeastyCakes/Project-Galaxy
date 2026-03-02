package com.projectorbit.data.mapper

import com.projectorbit.data.db.entity.BodyType as EntityBodyType
import com.projectorbit.data.db.entity.CelestialBodyEntity
import com.projectorbit.domain.model.BodyType as DomainBodyType
import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.util.Vec2

fun CelestialBodyEntity.toDomain(): CelestialBody = CelestialBody(
    id = id,
    parentId = parentId,
    type = type.toDomain(),
    name = name,
    position = Vec2(positionX, positionY),
    velocity = Vec2(velocityX, velocityY),
    mass = mass,
    radius = radius,
    orbitRadius = orbitRadius,
    orbitAngle = orbitAngle,
    isPinned = isPinned,
    isShared = isShared,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt,
    accessCount = accessCount,
    wordCount = wordCount,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun CelestialBody.toEntity(): CelestialBodyEntity = CelestialBodyEntity(
    id = id,
    parentId = parentId,
    type = type.toEntity(),
    name = name,
    positionX = position.x,
    positionY = position.y,
    velocityX = velocity.x,
    velocityY = velocity.y,
    mass = mass,
    radius = radius,
    orbitRadius = orbitRadius,
    orbitAngle = orbitAngle,
    isPinned = isPinned,
    isShared = isShared,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt,
    accessCount = accessCount,
    wordCount = wordCount,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

private fun EntityBodyType.toDomain(): DomainBodyType = when (this) {
    EntityBodyType.SUN -> DomainBodyType.SUN
    EntityBodyType.GAS_GIANT -> DomainBodyType.GAS_GIANT
    EntityBodyType.BINARY_STAR -> DomainBodyType.BINARY_STAR
    EntityBodyType.PLANET -> DomainBodyType.PLANET
    EntityBodyType.MOON -> DomainBodyType.MOON
    EntityBodyType.ASTEROID -> DomainBodyType.ASTEROID
    EntityBodyType.DWARF_PLANET -> DomainBodyType.DWARF_PLANET
    EntityBodyType.NEBULA -> DomainBodyType.NEBULA
}

private fun DomainBodyType.toEntity(): EntityBodyType = when (this) {
    DomainBodyType.SUN -> EntityBodyType.SUN
    DomainBodyType.GAS_GIANT -> EntityBodyType.GAS_GIANT
    DomainBodyType.BINARY_STAR -> EntityBodyType.BINARY_STAR
    DomainBodyType.PLANET -> EntityBodyType.PLANET
    DomainBodyType.MOON -> EntityBodyType.MOON
    DomainBodyType.ASTEROID -> EntityBodyType.ASTEROID
    DomainBodyType.DWARF_PLANET -> EntityBodyType.DWARF_PLANET
    DomainBodyType.NEBULA -> EntityBodyType.NEBULA
}
