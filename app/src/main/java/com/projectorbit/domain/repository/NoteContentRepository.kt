package com.projectorbit.domain.repository

import com.projectorbit.domain.model.NoteContent

interface NoteContentRepository {
    suspend fun getForBody(bodyId: String): NoteContent?
    suspend fun upsert(content: NoteContent)
    suspend fun searchPlainText(query: String): List<String>
}
