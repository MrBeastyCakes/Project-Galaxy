package com.projectorbit.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "links",
    indices = [Index(value = ["sourceId"]), Index(value = ["targetId"])],
    foreignKeys = [
        ForeignKey(
            entity = CelestialBodyEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CelestialBodyEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LinkEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val targetId: String,
    val linkType: LinkType,            // CONSTELLATION (backlink), TIDAL_LOCK, WORMHOLE
    val createdAt: Long
)

enum class LinkType { CONSTELLATION, TIDAL_LOCK, WORMHOLE }
