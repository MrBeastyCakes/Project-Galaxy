package com.projectorbit.ui.surface

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.projectorbit.ui.theme.OrbitSurface
import com.projectorbit.ui.theme.SpaceBlack
import com.projectorbit.ui.viewmodel.SurfaceEditorViewModel
import kotlinx.coroutines.launch

/**
 * Surface editor screen shown when zooming into a planet past the 50.0 threshold.
 * Uses zoom hysteresis: appears at 50.0, dismisses at 40.0.
 *
 * [visible] controls crossfade visibility from the parent galaxy screen.
 * [bodyId] and [bodyName] identify the planet being edited.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurfaceScreen(
    bodyId: String,
    bodyName: String,
    visible: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: SurfaceEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load note when body changes
    LaunchedEffect(bodyId) {
        viewModel.loadNote(bodyId, bodyName)
    }

    // Show error snackbar
    LaunchedEffect(uiState.saveError) {
        if (uiState.saveError) {
            snackbarHostState.showSnackbar("Unable to save")
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(uiState.bodyName, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = {
                            // Await save completion before navigating to prevent data loss race
                            scope.launch {
                                viewModel.saveNoteAndWait()
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.saveNote() }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                        Text(
                            text = "${uiState.wordCount} words",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OrbitSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = SpaceBlack
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                OrbitRichTextEditor(
                    initialJson = uiState.richTextJson,
                    onContentChanged = { json, plain ->
                        viewModel.updateContent(json, plain)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
