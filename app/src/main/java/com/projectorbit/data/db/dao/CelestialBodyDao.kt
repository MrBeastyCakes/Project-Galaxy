package com.projectorbit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.projectorbit.data.db.entity.BodyType
import com.projectorbit.data.db.entity.CelestialBodyEntity
import kotlinx.coroutines.flow.Flow

data class PhysicsStateUpdate(
    val id: String,
    val x: Double,
    val y: Double,
    val vx: Double,
    val vy: Double
)

@Dao
interface CelestialBodyDao {

    @Query("SELECT * FROM celestial_bodies WHERE isDeleted = 0")
    fun getAllActive(): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE parentId = :parentId AND isDeleted = 0")
    fun getChildrenOf(parentId: String): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE parentId = :parentId AND isDeleted = 0")
    fun observeChildren(parentId: String): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE parentId = :parentId AND type = :type AND isDeleted = 0")
    fun getChildrenOfByType(parentId: String, type: BodyType): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE type = 'SUN'")
    fun getAllSuns(): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE type = 'ASTEROID' AND isDeleted = 0")
    fun getAllAsteroids(): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE id = :id")
    suspend fun getById(id: String): CelestialBodyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(body: CelestialBodyEntity)

    @Query("UPDATE celestial_bodies SET positionX = :x, positionY = :y, velocityX = :vx, velocityY = :vy WHERE id = :id")
    suspend fun updatePhysicsState(id: String, x: Double, y: Double, vx: Double, vy: Double)

    @Transaction
    suspend fun updatePhysicsStateBatch(states: List<PhysicsStateUpdate>) {
        for (state in states) {
            updatePhysicsState(state.id, state.x, state.y, state.vx, state.vy)
        }
    }

    @Query("UPDATE celestial_bodies SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("UPDATE celestial_bodies SET mass = :mass, radius = :radius WHERE id = :id")
    suspend fun updateMassAndRadius(id: String, mass: Double, radius: Double)

    @Query("UPDATE celestial_bodies SET isCompleted = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun completeMoon(id: String, completedAt: Long)
}
