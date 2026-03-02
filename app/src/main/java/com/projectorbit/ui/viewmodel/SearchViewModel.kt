package com.projectorbit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectorbit.domain.repository.NoteContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val matchedBodyIds: Set<String> = emptySet(),
    val isTelescopeActive: Boolean = false,
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteContentRepository: NoteContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }

        if (query.isBlank()) {
            _uiState.update { it.copy(matchedBodyIds = emptySet(), isSearching = false) }
            return
        }

        // Debounce 300ms
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L)
            _uiState.update { it.copy(isSearching = true) }
            val results = noteContentRepository.searchPlainText(query)
            _uiState.update {
                it.copy(
                    matchedBodyIds = results.toSet(),
                    isSearching = false
                )
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            SearchUiState(isTelescopeActive = it.isTelescopeActive)
        }
    }

    fun toggleTelescope() {
        _uiState.update { it.copy(isTelescopeActive = !it.isTelescopeActive) }
        if (!_uiState.value.isTelescopeActive) {
            clearSearch()
        }
    }

    fun activateTelescope() {
        _uiState.update { it.copy(isTelescopeActive = true) }
    }

    fun deactivateTelescope() {
        _uiState.update { it.copy(isTelescopeActive = false) }
        clearSearch()
    }
}
