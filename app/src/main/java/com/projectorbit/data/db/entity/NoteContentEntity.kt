package com.projectorbit.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_contents",
    foreignKeys = [
        ForeignKey(
            entity = CelestialBodyEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteContentEntity(
    @PrimaryKey val bodyId: String,    // FK to celestial_bodies.id
    val richTextJson: String,          // Serialized rich text (spans, formatting)
    val plainText: String,             // Plain text for search indexing
    val updatedAt: Long
)
