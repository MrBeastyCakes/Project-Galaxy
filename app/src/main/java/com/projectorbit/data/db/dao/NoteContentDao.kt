package com.projectorbit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.projectorbit.data.db.entity.NoteContentEntity

@Dao
interface NoteContentDao {

    @Query("SELECT * FROM note_contents WHERE bodyId = :bodyId")
    suspend fun getForBody(bodyId: String): NoteContentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(content: NoteContentEntity)

    @Query("""
        SELECT bodyId FROM note_contents
        JOIN note_contents_fts ON note_contents.rowid = note_contents_fts.rowid
        WHERE note_contents_fts MATCH :query
    """)
    suspend fun searchPlainText(query: String): List<String>
}
