package com.projectorbit.data.db.fts

import androidx.room.Entity
import androidx.room.Fts4
import com.projectorbit.data.db.entity.NoteContentEntity

@Fts4(contentEntity = NoteContentEntity::class)
@Entity(tableName = "note_contents_fts")
data class NoteContentFts(
    val plainText: String              // FTS shadow of note_contents.plainText
)
