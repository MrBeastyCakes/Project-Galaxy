package com.projectorbit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.projectorbit.ui.canvas.GalaxySurfaceView
import com.projectorbit.ui.contextmenu.BodyContextMenu
import com.projectorbit.ui.contextmenu.MenuAction
import com.projectorbit.ui.quickcapture.QuickCaptureSheet
import com.projectorbit.ui.search.TelescopeOverlay
import com.projectorbit.ui.surface.SurfaceScreen
import com.projectorbit.ui.viewmodel.GalaxyViewModel
import com.projectorbit.ui.viewmodel.SearchViewModel

/**
 * Main screen hosting the galaxy canvas (GalaxySurfaceView) with Compose overlays:
 *  - TelescopeOverlay (search, shown when telescope active)
 *  - BodyContextMenu (on body selection)
 *  - SurfaceScreen (rich text editor, shown at zoom >= 50)
 *  - QuickCaptureSheet (FAB, bottom sheet)
 *  - Delete undo Snackbar (30-second window)
 *  - First-launch empty-state prompt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxyScreen(
    galaxyViewModel: GalaxyViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by galaxyViewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showQuickCapture by remember { mutableStateOf(false) }

    // Delete undo snackbar
    LaunchedEffect(uiState.pendingUndoBodyId) {
        val bodyId = uiState.pendingUndoBodyId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Note deleted",
            actionLabel = "Undo",
            withDismissAction = false,
            duration = androidx.compose.material3.SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            galaxyViewModel.undoDelete(bodyId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Black,
        floatingActionButton = {
            if (!uiState.isSurfaceEditorVisible && !searchState.isTelescopeActive) {
                FloatingActionButton(
                    onClick = { showQuickCapture = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Quick capture")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Galaxy Canvas (GalaxySurfaceView) — base layer ---
            AndroidView(
                factory = { context ->
                    GalaxySurfaceView(context).also { view ->
                        // Surface is not ready at factory time; wire gestures after the first
                        // layout pass when surfaceChanged has run and camera exists.
                        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                            view.setupGestures(
                                physicsWorld = galaxyViewModel.physicsWorld,
                                onBodySelected = { body ->
                                    galaxyViewModel.selectBody(body?.id)
                                },
                                onDoubleTapBody = { body ->
                                    galaxyViewModel.zoomToBody(body)
                                },
                                onDoubleTapEmpty = {
                                    galaxyViewModel.zoomOut()
                                },
                                onAccretionDrop = { asteroidId, planetId ->
                                    // Handled by ViewModel; MergeAsteroidUseCase called there
                                    galaxyViewModel.mergeAsteroidIntoPlanet(asteroidId, planetId)
                                },
                                onCreatePlanetFromAsteroid = { asteroidId, worldX, worldY ->
                                    galaxyViewModel.convertAsteroidToPlanet(asteroidId, worldX, worldY)
                                },
                                onCreateTidalLock = { bodyIdA, bodyIdB ->
                                    galaxyViewModel.createTidalLock(bodyIdA, bodyIdB)
                                },
                                onZoomChanged = { zoom ->
                                    galaxyViewModel.onZoomChanged(zoom)
                                }
                            )
                        }
                    }
                },
                update = { view ->
                    // Push latest render snapshot to the SurfaceView each recompose
                    view.postSnapshot(uiState.renderSnapshot)

                    // Feed physics snapshot to gesture hit-testing
                    view.updateGestureSnapshot(galaxyViewModel.physicsWorld.getSnapshot())

                    // Sync telescope state
                    view.setTelescopeState(
                        active = searchState.isTelescopeActive,
                        highlightIds = searchState.matchedBodyIds
                    )
                    if (searchState.isTelescopeActive) {
                        // Center the beam at the viewport center
                        view.setTelescopeCenter(
                            view.width / 2f,
                            view.height / 2f
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- First-launch prompt ---
            if (uiState.isFirstLaunch && !uiState.isSurfaceEditorVisible) {
                Text(
                    text = "Tap + to create your first Sun (category)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            }

            // --- Context menu (shown when a body is selected) ---
            val selectedId = uiState.selectedBodyId
            if (selectedId != null && !uiState.isSurfaceEditorVisible && !searchState.isTelescopeActive) {
                val selectedBody = uiState.renderSnapshot.bodies.find { it.id == selectedId }
                if (selectedBody != null) {
                    val camera = galaxyViewModel.camera
                    if (camera != null) {
                        val (sx, sy) = camera.worldToScreen(selectedBody.positionX, selectedBody.positionY)
                        BodyContextMenu(
                            screenX = sx,
                            screenY = sy,
                            bodyType = selectedBody.bodyType,
                            isPinned = selectedBody.isPinned,
                            isShared = selectedBody.isShared,
                            onAction = { action ->
                                when (action) {
                                    MenuAction.OPEN -> {
                                        // Zoom to body to trigger surface editor
                                        val snap = uiState.renderSnapshot.bodies.find { it.id == selectedId }
                                        if (snap != null) {
                                            camera.animateTo(snap.positionX, snap.positionY, 55f)
                                        }
                                    }
                                    MenuAction.DELETE -> {
                                        galaxyViewModel.deleteBody(
                                            bodyId = selectedId,
                                            plainText = "",
                                            posX = selectedBody.positionX,
                                            posY = selectedBody.positionY
                                        )
                                    }
                                    MenuAction.PIN, MenuAction.UNPIN -> {
                                        galaxyViewModel.pinBody(selectedId)
                                    }
                                    MenuAction.RENAME -> {
                                        galaxyViewModel.showRenameDialog(selectedId)
                                    }
                                    MenuAction.ADD_CHILD -> {
                                        galaxyViewModel.showAddChildDialog(selectedId)
                                    }
                                    MenuAction.SHARE, MenuAction.UNSHARE -> {
                                        galaxyViewModel.shareBody(selectedId)
                                    }
                                    MenuAction.LINK, MenuAction.TAG, MenuAction.CREATE_WORMHOLE -> {
                                        // Advanced actions: deselect for now; future screens will handle these
                                    }
                                }
                                galaxyViewModel.deselectBody()
                            },
                            onDismiss = { galaxyViewModel.deselectBody() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // --- Surface editor overlay ---
            if (uiState.isSurfaceEditorVisible && selectedId != null) {
                val selectedBody = uiState.renderSnapshot.bodies.find { it.id == selectedId }
                SurfaceScreen(
                    bodyId = selectedId,
                    bodyName = selectedBody?.name ?: "",
                    visible = uiState.isSurfaceEditorVisible,
                    onNavigateBack = { galaxyViewModel.zoomOut() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- Telescope overlay (search UI) ---
            if (searchState.isTelescopeActive) {
                TelescopeOverlay(
                    query = searchState.query,
                    onQueryChange = { searchViewModel.search(it) },
                    resultCount = searchState.matchedBodyIds.size,
                    onDismiss = { searchViewModel.deactivateTelescope() }
                )
            } else {
                // Search icon (top-right, shown when telescope is closed)
                IconButton(
                    onClick = { searchViewModel.toggleTelescope() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Telescope search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Quick capture bottom sheet
    if (showQuickCapture) {
        QuickCaptureSheet(
            onCapture = { text ->
                galaxyViewModel.createAsteroid(text)
                showQuickCapture = false
            },
            onDismiss = { showQuickCapture = false }
        )
    }

    // Rename dialog
    val renameBodyId = uiState.showRenameDialogForId
    if (renameBodyId != null) {
        val currentName = uiState.renderSnapshot.bodies.find { it.id == renameBodyId }?.name ?: ""
        var nameInput by remember(renameBodyId) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { galaxyViewModel.dismissRenameDialog() },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { galaxyViewModel.renameBody(renameBodyId, nameInput.trim()) },
                    enabled = nameInput.isNotBlank()
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { galaxyViewModel.dismissRenameDialog() }) { Text("Cancel") }
            }
        )
    }

    // Add child dialog
    val addChildParentId = uiState.showAddChildDialogForId
    if (addChildParentId != null) {
        var childName by remember(addChildParentId) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { galaxyViewModel.dismissAddChildDialog() },
            title = { Text("Add Child") },
            text = {
                OutlinedTextField(
                    value = childName,
                    onValueChange = { childName = it },
                    placeholder = { Text("Planet name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { galaxyViewModel.addChildPlanet(addChildParentId, childName.trim()) },
                    enabled = childName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { galaxyViewModel.dismissAddChildDialog() }) { Text("Cancel") }
            }
        )
    }
}
