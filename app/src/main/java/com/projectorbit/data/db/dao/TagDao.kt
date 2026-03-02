package com.projectorbit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.projectorbit.data.db.entity.BodyTagCrossRef
import com.projectorbit.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRef(crossRef: BodyTagCrossRef)

    @Query("SELECT t.* FROM tags t INNER JOIN body_tags bt ON t.id = bt.tagId WHERE bt.bodyId = :bodyId")
    fun getTagsForBody(bodyId: String): Flow<List<TagEntity>>

    @Query("DELETE FROM body_tags WHERE bodyId = :bodyId AND tagId = :tagId")
    suspend fun removeTagFromBody(bodyId: String, tagId: String)
}
