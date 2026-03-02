package com.projectorbit.data.repository

import com.projectorbit.data.db.dao.LinkDao
import com.projectorbit.data.mapper.toDomain
import com.projectorbit.data.mapper.toEntity
import com.projectorbit.domain.model.Link
import com.projectorbit.domain.model.LinkType
import com.projectorbit.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LinkRepositoryImpl @Inject constructor(
    private val dao: LinkDao
) : LinkRepository {

    override fun getAllLinks(): Flow<List<Link>> =
        dao.getAllLinks().map { list -> list.map { it.toDomain() } }

    override fun getLinksForBody(bodyId: String): Flow<List<Link>> =
        dao.getLinksForBody(bodyId).map { list -> list.map { it.toDomain() } }

    override fun getAllConstellations(): Flow<List<Link>> =
        dao.getAllConstellations().map { list -> list.map { it.toDomain() } }

    override fun getAllTidalLocks(): Flow<List<Link>> =
        dao.getAllTidalLocks().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(link: Link) =
        dao.upsert(link.toEntity())

    override suspend fun delete(link: Link) =
        dao.delete(link.toEntity())

    override suspend fun deleteByEndpoints(sourceId: String, targetId: String, linkType: LinkType) =
        dao.deleteByEndpoints(sourceId, targetId, linkType.name)
}
