package com.projectorbit.data.repository

import com.projectorbit.data.db.dao.TagDao
import com.projectorbit.data.db.entity.BodyTagCrossRef
import com.projectorbit.data.mapper.toDomain
import com.projectorbit.data.mapper.toEntity
import com.projectorbit.domain.model.Tag
import com.projectorbit.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val dao: TagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> =
        dao.getAllTags().map { list -> list.map { it.toDomain() } }

    override fun getTagsForBody(bodyId: String): Flow<List<Tag>> =
        dao.getTagsForBody(bodyId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(tag: Tag) =
        dao.upsert(tag.toEntity())

    override suspend fun addTagToBody(bodyId: String, tagId: String) =
        dao.upsertCrossRef(BodyTagCrossRef(bodyId = bodyId, tagId = tagId))

    override suspend fun removeTagFromBody(bodyId: String, tagId: String) =
        dao.removeTagFromBody(bodyId, tagId)
}
