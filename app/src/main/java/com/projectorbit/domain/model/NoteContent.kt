package com.projectorbit.domain.model

/**
 * Domain model for the rich text content of a note (planet/surface).
 */
data class NoteContent(
    val bodyId: String,
    val richTextJson: String,
    val plainText: String,
    val updatedAt: Long
)
