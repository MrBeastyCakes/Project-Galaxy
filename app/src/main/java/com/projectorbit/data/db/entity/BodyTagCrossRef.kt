package com.projectorbit.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "body_tags",
    primaryKeys = ["bodyId", "tagId"],
    indices = [Index(value = ["bodyId"]), Index(value = ["tagId"])],
    foreignKeys = [
        ForeignKey(
            entity = CelestialBodyEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BodyTagCrossRef(
    val bodyId: String,
    val tagId: String
)
