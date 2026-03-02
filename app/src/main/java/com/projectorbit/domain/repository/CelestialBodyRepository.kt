package com.projectorbit.domain.repository

import com.projectorbit.domain.model.CelestialBody
import kotlinx.coroutines.flow.Flow

data class PhysicsStateUpdate(
    val id: String,
    val x: Double,
    val y: Double,
    val vx: Double,
    val vy: Double
)

interface CelestialBodyRepository {
    fun getAllActive(): Flow<List<CelestialBody>>
    fun getChildrenOf(parentId: String): Flow<List<CelestialBody>>
    fun observeChildren(parentId: String): Flow<List<CelestialBody>>
    fun getAllSuns(): Flow<List<CelestialBody>>
    fun getAllAsteroids(): Flow<List<CelestialBody>>
    suspend fun getById(id: String): CelestialBody?
    suspend fun upsert(body: CelestialBody)
    suspend fun updatePhysicsStateBatch(states: List<PhysicsStateUpdate>)
    suspend fun softDelete(id: String, deletedAt: Long)
    suspend fun updateMassAndRadius(id: String, mass: Double, radius: Double)
    suspend fun completeMoon(id: String, completedAt: Long)
}
