package com.projectorbit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.model.CelestialBody
import com.projectorbit.domain.model.Link
import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.domain.repository.CelestialBodyRepository
import com.projectorbit.domain.repository.LinkRepository
import com.projectorbit.domain.usecase.CreateAsteroidUseCase
import com.projectorbit.domain.usecase.CreatePlanetUseCase
import com.projectorbit.domain.usecase.CreateSunUseCase
import com.projectorbit.domain.repository.NoteContentRepository
import com.projectorbit.domain.usecase.CreateTidalLockUseCase
import com.projectorbit.domain.usecase.DeleteBodyUseCase
import com.projectorbit.domain.usecase.MergeAsteroidUseCase
import com.projectorbit.domain.usecase.ToggleMoonUseCase
import com.projectorbit.ui.canvas.Camera
import com.projectorbit.ui.canvas.RenderSnapshot
import com.projectorbit.ui.canvas.RenderSnapshotBuilder
import com.projectorbit.util.Vec2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.PI

data class GalaxyUiState(
    val renderSnapshot: RenderSnapshot = RenderSnapshot.EMPTY,
    val selectedBodyId: String? = null,
    val pendingUndoBodyId: String? = null,
    val isFirstLaunch: Boolean = false,
    val isSurfaceEditorVisible: Boolean = false,
    val currentZoom: Float = 1f,
    val morphPlanetColor: Int = 0,
    val morphPlanetRadius: Float = 60f,
    // Dialog state for inline actions
    val showRenameDialogForId: String? = null,
    val showAddChildDialogForId: String? = null
)

@HiltViewModel
class GalaxyViewModel @Inject constructor(
    val physicsWorld: PhysicsWorld,
    private val repository: CelestialBodyRepository,
    private val noteContentRepository: NoteContentRepository,
    private val linkRepository: LinkRepository,
    private val createSunUseCase: CreateSunUseCase,
    private val createPlanetUseCase: CreatePlanetUseCase,
    private val createAsteroidUseCase: CreateAsteroidUseCase,
    private val deleteBodyUseCase: DeleteBodyUseCase,
    private val mergeAsteroidUseCase: MergeAsteroidUseCase,
    private val createTidalLockUseCase: CreateTidalLockUseCase,
    private val toggleMoonUseCase: ToggleMoonUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalaxyUiState())
    val uiState: StateFlow<GalaxyUiState> = _uiState.asStateFlow()

    // Latest body map for render snapshot merging — updated from repository Flow
    private val _bodyMap = MutableStateFlow<Map<String, CelestialBody>>(emptyMap())
    private val _links = MutableStateFlow<List<Link>>(emptyList())

    // Camera is held here so it survives configuration changes.
    // GalaxySurfaceView reads it via the ViewModel.
    @Volatile
    var camera: Camera? = null
        private set

    init {
        // Observe all active bodies and build a lookup map
        viewModelScope.launch {
            var isFirstEmit = true
            repository.getAllActive().collect { list ->
                _bodyMap.value = list.associateBy { it.id }
                _uiState.update { it.copy(isFirstLaunch = list.isEmpty()) }
                // Fit camera on initial load if there are saved bodies
                if (isFirstEmit && list.isNotEmpty()) {
                    // Small delay to ensure camera is initialized
                    delay(100L)
                    fitCameraToAllBodies()
                }
                isFirstEmit = false
            }
        }

        // Observe all links (constellations + tidal locks combined)
        viewModelScope.launch {
            combine(
                linkRepository.getAllConstellations(),
                linkRepository.getAllTidalLocks()
            ) { constellations, tidalLocks ->
                constellations + tidalLocks
            }.collect { list ->
                _links.value = list
            }
        }

        // Poll PhysicsWorld snapshot at ~60Hz and merge into RenderSnapshot
        viewModelScope.launch {
            while (isActive) {
                val physicsSnap = physicsWorld.getSnapshot()
                val bodyMap = _bodyMap.value
                val links = _links.value
                val selectedId = _uiState.value.selectedBodyId

                val renderSnap = RenderSnapshotBuilder.build(
                    physicsSnapshot = physicsSnap,
                    bodyMap = bodyMap,
                    links = links,
                    selectedBodyId = selectedId
                )
                _uiState.update { it.copy(renderSnapshot = renderSnap) }
                delay(16L) // ~60fps
            }
        }
    }

    /**
     * Create or update the ViewModel-owned Camera. Returns the authoritative Camera instance
     * so GalaxyScreen can attach it to GalaxySurfaceView, ensuring rendering, gestures, and
     * ViewModel camera operations (zoomToBody, fitCameraToAllBodies) all share one object.
     */
    fun initCamera(screenWidth: Float, screenHeight: Float): Camera {
        val existing = camera
        if (existing != null) {
            // Update screen dimensions on orientation change while preserving camera state.
            if (existing.screenWidth != screenWidth || existing.screenHeight != screenHeight) {
                val updated = existing.onSurfaceSizeChanged(screenWidth, screenHeight)
                camera = updated
                return updated
            }
            return existing
        }
        val cam = Camera(screenWidth, screenHeight)
        camera = cam
        return cam
    }

    /**
     * Fit camera to show all bodies with padding.
     * Computes a bounding box of all active bodies and sets the camera
     * zoom and center to show them all.
     */
    fun fitCameraToAllBodies() {
        val cam = camera ?: return
        val bodies = _bodyMap.value.values.toList()
        if (bodies.isEmpty()) return

        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for (body in bodies) {
            val r = body.radius + 20.0 // padding around each body
            if (body.position.x - r < minX) minX = body.position.x - r
            if (body.position.x + r > maxX) maxX = body.position.x + r
            if (body.position.y - r < minY) minY = body.position.y - r
            if (body.position.y + r > maxY) maxY = body.position.y + r
        }

        val worldWidth = (maxX - minX).coerceAtLeast(200.0)
        val worldHeight = (maxY - minY).coerceAtLeast(200.0)
        val centerX = (minX + maxX) / 2.0
        val centerY = (minY + maxY) / 2.0

        // Calculate zoom to fit all bodies with 20% padding
        val screenW = cam.screenWidth
        val screenH = cam.screenHeight
        val zoomX = screenW / (worldWidth * 1.4) // 1.4 = 1.0 + 40% padding
        val zoomY = screenH / (worldHeight * 1.4)
        val targetZoom = minOf(zoomX, zoomY).toFloat().coerceIn(cam.minZoom, cam.maxZoom)

        cam.animateTo(centerX, centerY, targetZoom)
    }

    fun selectBody(bodyId: String?) {
        _uiState.update { it.copy(selectedBodyId = bodyId) }
    }

    fun deselectBody() {
        _uiState.update { it.copy(selectedBodyId = null) }
    }

    fun onZoomChanged(zoom: Float) {
        val current = _uiState.value.isSurfaceEditorVisible
        val newVisible = when {
            current && zoom > 30.0f -> true
            !current && zoom >= 50.0f -> true
            zoom <= 30.0f -> false
            else -> current
        }

        // Capture planet color when entering the morph zone (zoom 40+) for the first time
        val state = _uiState.value
        val needsCapture = zoom >= 40f && state.morphPlanetColor == 0 && state.selectedBodyId != null
        val capturedColor = if (needsCapture) {
            val body = state.renderSnapshot.bodies.find { it.id == state.selectedBodyId }
            body?.color ?: 0xFF508CC8.toInt()
        } else state.morphPlanetColor

        // Reset morph color when fully zoomed out of morph zone
        val morphColor = if (zoom < 38f) 0 else capturedColor

        _uiState.update { it.copy(
            currentZoom = zoom,
            isSurfaceEditorVisible = newVisible,
            morphPlanetColor = morphColor,
            morphPlanetRadius = state.morphPlanetRadius
        ) }
    }

    fun createSun(name: String) {
        viewModelScope.launch {
            // Spread suns apart: place each new sun 400 units from existing ones
            val existingSuns = _bodyMap.value.values.filter {
                it.type == BodyType.SUN
            }
            val sunCount = existingSuns.size
            val spacing = 400.0
            val posX = if (sunCount == 0) 0.0 else {
                val angle = sunCount * (2.0 * Math.PI / 6.0) // hexagonal spread
                kotlin.math.cos(angle) * spacing * ((sunCount + 1) / 2)
            }
            val posY = if (sunCount == 0) 0.0 else {
                val angle = sunCount * (2.0 * Math.PI / 6.0)
                kotlin.math.sin(angle) * spacing * ((sunCount + 1) / 2)
            }
            val newSun = createSunUseCase(name, posX, posY)
            camera?.animateTo(newSun.position.x, newSun.position.y, Camera.ZOOM_SYSTEM_MIN)
        }
    }

    fun createPlanet(
        name: String,
        parentId: String,
        parentX: Double,
        parentY: Double,
        orbitRadius: Double = 150.0
    ) {
        viewModelScope.launch {
            val orbitAngle = (Math.random() * 2 * PI)
            val newPlanet = createPlanetUseCase(name, parentId, parentX, parentY, orbitRadius, orbitAngle)
            camera?.animateTo(newPlanet.position.x, newPlanet.position.y, Camera.ZOOM_SYSTEM_MIN)
        }
    }

    fun createAsteroid(text: String = "") {
        viewModelScope.launch {
            val newAsteroid = createAsteroidUseCase(text)
            camera?.animateTo(newAsteroid.position.x, newAsteroid.position.y, Camera.ZOOM_CLUSTER_MAX)
        }
    }

    fun deleteBody(bodyId: String, posX: Double, posY: Double) {
        viewModelScope.launch {
            val plainText = noteContentRepository.getForBody(bodyId)?.plainText ?: ""
            deleteBodyUseCase(bodyId, plainText, posX, posY)
            _uiState.update { it.copy(pendingUndoBodyId = bodyId, selectedBodyId = null) }
            // Auto-clear undo after 30 seconds
            delay(30_000L)
            _uiState.update { state ->
                if (state.pendingUndoBodyId == bodyId) state.copy(pendingUndoBodyId = null)
                else state
            }
        }
    }

    fun undoDelete(bodyId: String) {
        viewModelScope.launch {
            val body = repository.getById(bodyId) ?: return@launch
            if (!body.isDeleted) return@launch
            val restored = body.copy(isDeleted = false, deletedAt = null)
            repository.upsert(restored)
            physicsWorld.addBody(
                com.projectorbit.domain.physics.PhysicsBody(
                    id = restored.id,
                    positionX = restored.position.x,
                    positionY = restored.position.y,
                    velocityX = restored.velocity.x,
                    velocityY = restored.velocity.y,
                    mass = restored.mass,
                    radius = restored.radius,
                    bodyType = restored.type,
                    parentId = restored.parentId,
                    orbitRadius = restored.orbitRadius
                )
            )
            _uiState.update { it.copy(pendingUndoBodyId = null) }
        }
    }

    fun showRenameDialog(bodyId: String) {
        _uiState.update { it.copy(showRenameDialogForId = bodyId) }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(showRenameDialogForId = null) }
    }

    fun renameBody(bodyId: String, newName: String) {
        viewModelScope.launch {
            val body = repository.getById(bodyId) ?: return@launch
            repository.upsert(body.copy(name = newName))
        }
        _uiState.update { it.copy(showRenameDialogForId = null) }
    }

    fun showAddChildDialog(bodyId: String) {
        _uiState.update { it.copy(showAddChildDialogForId = bodyId) }
    }

    fun dismissAddChildDialog() {
        _uiState.update { it.copy(showAddChildDialogForId = null) }
    }

    fun addChildPlanet(parentId: String, name: String) {
        val parentBody = _bodyMap.value[parentId] ?: return
        viewModelScope.launch {
            createPlanetUseCase(
                name = name,
                parentId = parentId,
                parentPositionX = parentBody.position.x,
                parentPositionY = parentBody.position.y
            )
        }
        _uiState.update { it.copy(showAddChildDialogForId = null) }
    }

    fun shareBody(bodyId: String) {
        viewModelScope.launch {
            val body = repository.getById(bodyId) ?: return@launch
            repository.upsert(body.copy(isShared = !body.isShared))
        }
    }

    fun pinBody(bodyId: String) {
        viewModelScope.launch {
            val body = repository.getById(bodyId) ?: return@launch
            val pinned = body.copy(isPinned = !body.isPinned)
            val newMass = CelestialBody.computeMass(pinned.wordCount, pinned.accessCount, pinned.isPinned)
            val newRadius = CelestialBody.computeRadius(newMass)
            repository.upsert(pinned.copy(mass = newMass, radius = newRadius))
            physicsWorld.setMass(bodyId, newMass)
        }
    }

    fun zoomToBody(body: BodySnapshot) {
        val cam = camera ?: return
        val targetZoom = when (body.bodyType) {
            BodyType.SUN -> Camera.ZOOM_SYSTEM_MIN
            BodyType.GAS_GIANT, BodyType.BINARY_STAR -> Camera.ZOOM_SYSTEM_MIN
            BodyType.PLANET, BodyType.DWARF_PLANET -> Camera.ZOOM_PLANET_MIN
            BodyType.MOON -> Camera.ZOOM_PLANET_MAX * 0.5f
            BodyType.ASTEROID -> Camera.ZOOM_CLUSTER_MAX
            BodyType.NEBULA -> Camera.ZOOM_CLUSTER_MAX
        }
        cam.animateTo(body.positionX, body.positionY, targetZoom)
    }

    fun zoomOut() {
        val cam = camera ?: return
        val currentZoom = cam.zoom
        val targetZoom = when {
            currentZoom >= Camera.ZOOM_SURFACE_MIN -> Camera.ZOOM_PLANET_MIN
            currentZoom >= Camera.ZOOM_PLANET_MIN -> Camera.ZOOM_SYSTEM_MIN
            currentZoom >= Camera.ZOOM_SYSTEM_MIN -> Camera.ZOOM_CLUSTER_MIN
            currentZoom >= Camera.ZOOM_CLUSTER_MIN -> Camera.ZOOM_GALAXY_MIN
            else -> cam.minZoom
        }
        cam.animateTo(cam.target.x, cam.target.y, targetZoom)
    }

    /** Merge an asteroid's content into a planet (accretion drop gesture). */
    fun mergeAsteroidIntoPlanet(asteroidId: String, planetId: String) {
        viewModelScope.launch {
            mergeAsteroidUseCase(asteroidId, planetId)
            _uiState.update { it.copy(selectedBodyId = null) }
        }
    }

    /**
     * Convert an asteroid dropped on empty space into a new planet at the drop position.
     * The asteroid becomes a planet: its content is preserved, type is upgraded.
     */
    fun convertAsteroidToPlanet(asteroidId: String, worldX: Double, worldY: Double) {
        viewModelScope.launch {
            val body = repository.getById(asteroidId) ?: return@launch
            val upgraded = body.copy(
                type = BodyType.PLANET,
                position = Vec2(worldX, worldY)
            )
            val newMass = CelestialBody.computeMass(
                upgraded.wordCount, upgraded.accessCount, upgraded.isPinned
            )
            val newRadius = CelestialBody.computeRadius(newMass)
            repository.upsert(upgraded.copy(mass = newMass, radius = newRadius))
            physicsWorld.setMass(asteroidId, newMass)
        }
    }

    /** Persist body position after drag and update orbit radius relative to parent. */
    fun onBodyDragged(bodyId: String, worldX: Double, worldY: Double) {
        viewModelScope.launch {
            val body = repository.getById(bodyId) ?: return@launch
            val updated = body.copy(position = Vec2(worldX, worldY))
            // Recalculate orbit radius if the body has a parent
            val parentId = body.parentId
            val newOrbitRadius = if (parentId != null) {
                val parent = repository.getById(parentId)
                if (parent != null) {
                    val dx = worldX - parent.position.x
                    val dy = worldY - parent.position.y
                    kotlin.math.sqrt(dx * dx + dy * dy)
                } else body.orbitRadius
            } else body.orbitRadius
            repository.upsert(updated.copy(orbitRadius = newOrbitRadius))
            physicsWorld.enqueue(
                com.projectorbit.domain.physics.PhysicsCommand.UpdateOrbitRadius(bodyId, newOrbitRadius)
            )
        }
    }

    /** Mark a moon as complete. Triggers dissolve animation and removes from physics. */
    fun toggleMoonCompletion(moonId: String) {
        viewModelScope.launch {
            toggleMoonUseCase(moonId)
            // Remove from physics after dissolve animation (1 second)
            delay(1000L)
            toggleMoonUseCase.removePhysicsBody(moonId)
            _uiState.update { it.copy(selectedBodyId = null) }
        }
    }

    /** Create a tidal lock constraint between two bodies (drag gesture). */
    fun createTidalLock(bodyIdA: String, bodyIdB: String) {
        viewModelScope.launch {
            createTidalLockUseCase(bodyIdA, bodyIdB)
        }
    }
}
