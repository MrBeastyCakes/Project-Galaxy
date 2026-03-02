package com.projectorbit.data.mapper

import com.projectorbit.data.db.entity.NoteContentEntity
import com.projectorbit.domain.model.NoteContent

fun NoteContentEntity.toDomain(): NoteContent = NoteContent(
    bodyId = bodyId,
    richTextJson = richTextJson,
    plainText = plainText,
    updatedAt = updatedAt
)

fun NoteContent.toEntity(): NoteContentEntity = NoteContentEntity(
    bodyId = bodyId,
    richTextJson = richTextJson,
    plainText = plainText,
    updatedAt = updatedAt
)
