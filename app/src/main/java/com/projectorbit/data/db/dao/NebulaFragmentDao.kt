package com.projectorbit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.projectorbit.data.db.entity.NebulaFragmentEntity

@Dao
interface NebulaFragmentDao {

    @Query("SELECT * FROM nebula_fragments WHERE textFragment LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<NebulaFragmentEntity>

    @Insert
    suspend fun insert(fragment: NebulaFragmentEntity)

    @Query("UPDATE nebula_fragments SET fadeFactor = :fadeFactor WHERE id = :id")
    suspend fun updateFade(id: String, fadeFactor: Double)

    @Query("DELETE FROM nebula_fragments WHERE fadeFactor <= 0.0")
    suspend fun deleteFullyFaded()
}
