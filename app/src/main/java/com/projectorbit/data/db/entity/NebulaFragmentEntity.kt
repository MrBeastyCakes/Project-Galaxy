package com.projectorbit.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nebula_fragments")
data class NebulaFragmentEntity(
    @PrimaryKey val id: String,
    val originalBodyId: String,        // ID of the deleted body
    val textFragment: String,          // Searchable remnant
    val positionX: Double,
    val positionY: Double,
    val createdAt: Long,               // When the supernova occurred
    val fadeFactor: Double             // 1.0 fresh -> 0.0 fully faded
)
