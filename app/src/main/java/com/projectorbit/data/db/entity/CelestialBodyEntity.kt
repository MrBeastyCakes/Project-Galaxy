package com.projectorbit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "celestial_bodies",
    indices = [Index(value = ["parentId"])]
)
data class CelestialBodyEntity(
    @PrimaryKey val id: String,           // UUID
    val parentId: String?,                // null for Suns; parent body ID for everything else
    val type: BodyType,                   // SUN, GAS_GIANT, PLANET, MOON, ASTEROID, DWARF_PLANET
    val name: String,
    val positionX: Double,                // World-space position (Double for precision)
    val positionY: Double,
    val velocityX: Double,               // Current orbital velocity
    val velocityY: Double,
    val mass: Double,                    // Derived from importance/word count/pin status
    val radius: Double,                  // Visual radius (derived from mass)
    val orbitRadius: Double,             // Distance from parent (target orbit)
    val orbitAngle: Double,              // Current angle around parent (used by moons)
    val isPinned: Boolean,
    val isShared: Boolean = false,       // When true, renders ring system
    val isCompleted: Boolean = false,    // Only used when type = MOON (sub-task completion)
    val completedAt: Long? = null,       // Only used when type = MOON
    val createdAt: Long,                 // Epoch millis
    val lastAccessedAt: Long,
    val accessCount: Int,
    val wordCount: Int,
    val isDeleted: Boolean,              // Soft delete (supernova -> nebula)
    val deletedAt: Long?
)

enum class BodyType { SUN, GAS_GIANT, BINARY_STAR, PLANET, MOON, ASTEROID, DWARF_PLANET, NEBULA }
