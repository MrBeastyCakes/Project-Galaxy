package com.projectorbit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.projectorbit.data.db.entity.LinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {

    @Query("SELECT * FROM links")
    fun getAllLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE sourceId = :bodyId OR targetId = :bodyId")
    fun getLinksForBody(bodyId: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE linkType = 'CONSTELLATION'")
    fun getAllConstellations(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE linkType = 'TIDAL_LOCK'")
    fun getAllTidalLocks(): Flow<List<LinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: LinkEntity)

    @Delete
    suspend fun delete(link: LinkEntity)

    @Query("DELETE FROM links WHERE sourceId = :sourceId AND targetId = :targetId AND linkType = :linkType")
    suspend fun deleteByEndpoints(sourceId: String, targetId: String, linkType: String)
}
