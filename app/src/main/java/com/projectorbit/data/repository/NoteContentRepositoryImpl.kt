package com.projectorbit.data.repository

import com.projectorbit.data.db.dao.NoteContentDao
import com.projectorbit.data.mapper.toDomain
import com.projectorbit.data.mapper.toEntity
import com.projectorbit.domain.model.NoteContent
import com.projectorbit.domain.repository.NoteContentRepository
import javax.inject.Inject

class NoteContentRepositoryImpl @Inject constructor(
    private val dao: NoteContentDao
) : NoteContentRepository {

    override suspend fun getForBody(bodyId: String): NoteContent? =
        dao.getForBody(bodyId)?.toDomain()

    override suspend fun upsert(content: NoteContent) =
        dao.upsert(content.toEntity())

    override suspend fun searchPlainText(query: String): List<String> =
        dao.searchPlainText(query)
}
