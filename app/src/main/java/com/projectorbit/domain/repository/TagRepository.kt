package com.projectorbit.domain.repository

import com.projectorbit.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    fun getTagsForBody(bodyId: String): Flow<List<Tag>>
    suspend fun upsert(tag: Tag)
    suspend fun addTagToBody(bodyId: String, tagId: String)
    suspend fun removeTagFromBody(bodyId: String, tagId: String)
}
