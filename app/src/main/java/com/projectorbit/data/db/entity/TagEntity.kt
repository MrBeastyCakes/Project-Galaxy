package com.projectorbit.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,                  // e.g. "Urgent", "Reference"
    val atmosphereColor: Int,          // ARGB packed int
    val atmosphereDensity: Double      // 0.0 (faint) to 1.0 (opaque)
)
