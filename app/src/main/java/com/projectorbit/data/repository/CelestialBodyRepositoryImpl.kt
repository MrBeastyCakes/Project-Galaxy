package com.projectorbit.data.repository

import com.projectorbit.data.db.dao.CelestialBodyDao
import com.projectorbit.data.db.dao.PhysicsStateUpdate as DaoPhysicsStateUpdate
import com.projectorbit.data.mapper.toDomain
import com.projectorbit.data.mapper.toEntity
import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.PhysicsStateUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CelestialBodyRepositoryImpl @Inject constructor(
    private val dao: CelestialBodyDao
) : CelestialBodyRepository {

    override fun getAllActive(): Flow<List<CelestialBody>> =
        dao.getAllActive().map { list -> list.map { it.toDomain() } }

    override fun getChildrenOf(parentId: String): Flow<List<CelestialBody>> =
        dao.getChildrenOf(parentId).map { list -> list.map { it.toDomain() } }

    override fun observeChildren(parentId: String): Flow<List<CelestialBody>> =
        dao.observeChildren(parentId).map { list -> list.map { it.toDomain() } }

    override fun getAllSuns(): Flow<List<CelestialBody>> =
        dao.getAllSuns().map { list -> list.map { it.toDomain() } }

    override fun getAllAsteroids(): Flow<List<CelestialBody>> =
        dao.getAllAsteroids().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): CelestialBody? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(body: CelestialBody) =
        dao.upsert(body.toEntity())

    override suspend fun updatePhysicsStateBatch(states: List<PhysicsStateUpdate>) {
        dao.updatePhysicsStateBatch(states.map { s ->
            DaoPhysicsStateUpdate(id = s.id, x = s.x, y = s.y, vx = s.vx, vy = s.vy)
        })
    }

    override suspend fun softDelete(id: String, deletedAt: Long) =
        dao.softDelete(id, deletedAt)

    override suspend fun updateMassAndRadius(id: String, mass: Double, radius: Double) =
        dao.updateMassAndRadius(id, mass, radius)

    override suspend fun completeMoon(id: String, completedAt: Long) =
        dao.completeMoon(id, completedAt)
}
