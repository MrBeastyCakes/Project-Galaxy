package com.projectorbit.domain.repository

import com.projectorbit.domain.model.Link
import com.projectorbit.domain.model.LinkType
import kotlinx.coroutines.flow.Flow

interface LinkRepository {
    fun getAllLinks(): Flow<List<Link>>
    fun getLinksForBody(bodyId: String): Flow<List<Link>>
    fun getAllConstellations(): Flow<List<Link>>
    fun getAllTidalLocks(): Flow<List<Link>>
    suspend fun upsert(link: Link)
    suspend fun delete(link: Link)
    suspend fun deleteByEndpoints(sourceId: String, targetId: String, linkType: LinkType)
}
