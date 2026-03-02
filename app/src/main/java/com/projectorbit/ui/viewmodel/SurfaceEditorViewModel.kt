package com.projectorbit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.model.NoteContent
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.NoteContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val bodyId: String? = null,
    val bodyName: String = "",
    val richTextJson: String = "",
    val plainText: String = "",
    val wordCount: Int = 0,
    val isSaving: Boolean = false,
    val saveError: Boolean = false
)

@HiltViewModel
class SurfaceEditorViewModel @Inject constructor(
    private val noteContentRepository: NoteContentRepository,
    private val celestialBodyRepository: CelestialBodyRepository,
    private val physicsWorld: PhysicsWorld
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun loadNote(bodyId: String, bodyName: String) {
        viewModelScope.launch {
            val content = noteContentRepository.getForBody(bodyId)
            _uiState.update {
                it.copy(
                    bodyId = bodyId,
                    bodyName = bodyName,
                    richTextJson = content?.richTextJson ?: "",
                    plainText = content?.plainText ?: "",
                    wordCount = content?.plainText?.split("\\s+".toRegex())
                        ?.count { w -> w.isNotEmpty() } ?: 0
                )
            }
        }
    }

    fun updateContent(richTextJson: String, plainText: String) {
        val wordCount = plainText.split("\\s+".toRegex()).count { it.isNotEmpty() }
        _uiState.update {
            it.copy(
                richTextJson = richTextJson,
                plainText = plainText,
                wordCount = wordCount
            )
        }
    }

    fun saveNote() {
        val state = _uiState.value
        val bodyId = state.bodyId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = false) }
            try {
                val now = System.currentTimeMillis()
                noteContentRepository.upsert(
                    NoteContent(
                        bodyId = bodyId,
                        richTextJson = state.richTextJson,
                        plainText = state.plainText,
                        updatedAt = now
                    )
                )

                // Update word count and mass on the celestial body
                val body = celestialBodyRepository.getById(bodyId)
                if (body != null) {
                    val newMass = CelestialBody.computeMass(
                        wordCount = state.wordCount,
                        accessCount = body.accessCount,
                        isPinned = body.isPinned
                    )
                    val newRadius = CelestialBody.computeRadius(newMass)
                    celestialBodyRepository.updateMassAndRadius(bodyId, newMass, newRadius)
                    physicsWorld.setMass(bodyId, newMass)
                }

                _uiState.update { it.copy(isSaving = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }
}
