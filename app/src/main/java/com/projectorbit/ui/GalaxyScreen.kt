package com.projectorbit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
    var showCreateMenu by remember { mutableStateOf(false) }
    var showCreateSunDialog by remember { mutableStateOf(false) }
    var showCreatePlanetDialog by remember { mutableStateOf(false) }

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
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Expandable creation options
                    AnimatedVisibility(
                        visible = showCreateMenu,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Create Sun (category)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Sun (Category)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                SmallFloatingActionButton(
                                    onClick = {
                                        showCreateMenu = false
                                        showCreateSunDialog = true
                                    },
                                    containerColor = androidx.compose.ui.graphics.Color(0xFFFFDC50)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Create Sun",
                                        tint = androidx.compose.ui.graphics.Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Create Planet (note under a sun)
                            if (uiState.renderSnapshot.bodies.any {
                                    it.bodyType == com.projectorbit.domain.model.BodyType.SUN
                                }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Planet (Note)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    SmallFloatingActionButton(
                                        onClick = {
                                            showCreateMenu = false
                                            showCreatePlanetDialog = true
                                        },
                                        containerColor = androidx.compose.ui.graphics.Color(0xFF508CC8)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Create Planet",
                                            tint = androidx.compose.ui.graphics.Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Quick Capture (asteroid)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Quick Thought",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                SmallFloatingActionButton(
                                    onClick = {
                                        showCreateMenu = false
                                        showQuickCapture = true
                                    },
                                    containerColor = androidx.compose.ui.graphics.Color(0xFF8C8278)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Quick Capture",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = { showCreateMenu = !showCreateMenu },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            if (showCreateMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (showCreateMenu) "Close menu" else "Create"
                        )
                    }
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
                                },
                                onBodyDragged = { bodyId, worldX, worldY ->
                                    galaxyViewModel.onBodyDragged(bodyId, worldX, worldY)
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

    // Create Sun dialog
    if (showCreateSunDialog) {
        var sunName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateSunDialog = false },
            title = { Text("Create Sun (Category)") },
            text = {
                Column {
                    Text(
                        "Suns are categories that anchor your galaxy. Planets orbit around them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.padding(top = 12.dp))
                    OutlinedTextField(
                        value = sunName,
                        onValueChange = { sunName = it },
                        placeholder = { Text("e.g. Work, Ideas, Journal...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        galaxyViewModel.createSun(sunName.trim())
                        showCreateSunDialog = false
                    },
                    enabled = sunName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSunDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create Planet dialog
    if (showCreatePlanetDialog) {
        val suns = uiState.renderSnapshot.bodies.filter {
            it.bodyType == com.projectorbit.domain.model.BodyType.SUN
        }
        var planetName by remember { mutableStateOf("") }
        var selectedSunIndex by remember { mutableStateOf(0) }
        AlertDialog(
            onDismissRequest = { showCreatePlanetDialog = false },
            title = { Text("Create Planet (Note)") },
            text = {
                Column {
                    Text(
                        "Planets are notes that orbit a Sun (category).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.padding(top = 12.dp))
                    OutlinedTextField(
                        value = planetName,
                        onValueChange = { planetName = it },
                        placeholder = { Text("Note title...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (suns.size > 1) {
                        Spacer(Modifier.padding(top = 8.dp))
                        Text(
                            "Orbiting: ${suns.getOrNull(selectedSunIndex)?.name ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            suns.forEachIndexed { index, sun ->
                                TextButton(
                                    onClick = { selectedSunIndex = index }
                                ) {
                                    Text(
                                        sun.name,
                                        color = if (index == selectedSunIndex)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parentSun = suns.getOrNull(selectedSunIndex)
                        if (parentSun != null) {
                            galaxyViewModel.createPlanet(
                                name = planetName.trim(),
                                parentId = parentSun.id,
                                parentX = parentSun.positionX,
                                parentY = parentSun.positionY
                            )
                        }
                        showCreatePlanetDialog = false
                    },
                    enabled = planetName.isNotBlank() && suns.isNotEmpty()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlanetDialog = false }) { Text("Cancel") }
            }
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
