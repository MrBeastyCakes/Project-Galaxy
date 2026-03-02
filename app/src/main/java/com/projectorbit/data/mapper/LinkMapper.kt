package com.projectorbit.data.mapper

import com.projectorbit.data.db.entity.LinkEntity
import com.projectorbit.data.db.entity.LinkType as EntityLinkType
import com.projectorbit.domain.model.Link
import com.projectorbit.domain.model.LinkType as DomainLinkType

fun LinkEntity.toDomain(): Link = Link(
    id = id,
    sourceId = sourceId,
    targetId = targetId,
    linkType = linkType.toDomain(),
    createdAt = createdAt
)

fun Link.toEntity(): LinkEntity = LinkEntity(
    id = id,
    sourceId = sourceId,
    targetId = targetId,
    linkType = linkType.toEntity(),
    createdAt = createdAt
)

private fun EntityLinkType.toDomain(): DomainLinkType = when (this) {
    EntityLinkType.CONSTELLATION -> DomainLinkType.CONSTELLATION
    EntityLinkType.TIDAL_LOCK -> DomainLinkType.TIDAL_LOCK
    EntityLinkType.WORMHOLE -> DomainLinkType.WORMHOLE
}

private fun DomainLinkType.toEntity(): EntityLinkType = when (this) {
    DomainLinkType.CONSTELLATION -> EntityLinkType.CONSTELLATION
    DomainLinkType.TIDAL_LOCK -> EntityLinkType.TIDAL_LOCK
    DomainLinkType.WORMHOLE -> EntityLinkType.WORMHOLE
}
