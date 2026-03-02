package com.projectorbit.data.mapper

import com.projectorbit.data.db.entity.TagEntity
import com.projectorbit.domain.model.Tag

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    atmosphereColor = atmosphereColor,
    atmosphereDensity = atmosphereDensity
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    atmosphereColor = atmosphereColor,
    atmosphereDensity = atmosphereDensity
)
