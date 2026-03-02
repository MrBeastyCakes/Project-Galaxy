package com.projectorbit.ui.canvas

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsSnapshot
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.ui.gesture.GestureRouter

/**
 * GalaxySurfaceView hosts the galaxy rendering pipeline and gesture routing.
 *
 * Owns the render thread lifecycle and provides a clean API for
 * the ViewModel/gesture layer to interact with the renderer.
 *
 * Physics snapshots arrive as RenderSnapshot (merged physics + visual data)
 * assembled by GalaxyViewModel before being posted here.
 *
 * Touch events are routed through [GestureRouter] to [PanZoomHandler],
 * [TapHandler], and [DragHandler].
 */
class GalaxySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    // Camera is shared: main thread writes targets, render thread reads and smooths
    var camera: Camera? = null
        private set

    private var renderer: GalaxyRenderer? = null
    private var renderThread: Thread? = null

    // Gesture router — wired via setupGestures() after surface is ready
    private var gestureRouter: GestureRouter? = null

    // Stored gesture callbacks so we can re-wire after surface recreation
    private var pendingGestureSetup: (() -> Unit)? = null

    init {
        holder.addCallback(this)
        setZOrderOnTop(false)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    // -------------------------------------------------------------------------
    // SurfaceHolder.Callback
    // -------------------------------------------------------------------------

    override fun surfaceCreated(holder: SurfaceHolder) {
        // surfaceChanged always follows; defer init there
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        stopRenderThread()

        val cam = Camera(width.toFloat(), height.toFloat())
        camera = cam

        val rend = GalaxyRenderer(holder, cam)
        rend.init(width, height)
        renderer = rend

        startRenderThread(rend)

        // Re-apply gesture setup now that camera is available
        pendingGestureSetup?.invoke()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopRenderThread()
        renderer = null
        camera = null
    }

    // -------------------------------------------------------------------------
    // Render thread lifecycle
    // -------------------------------------------------------------------------

    private fun startRenderThread(rend: GalaxyRenderer) {
        rend.running = true
        val thread = Thread({ rend.renderLoop() }, "GalaxyRenderThread")
        thread.priority = Thread.MAX_PRIORITY
        thread.start()
        renderThread = thread
    }

    private fun stopRenderThread() {
        renderer?.running = false
        renderThread?.join(500)
        renderThread = null
    }

    // -------------------------------------------------------------------------
    // Public API — called from ViewModel / gesture handlers on main thread
    // -------------------------------------------------------------------------

    /** Feed a new merged render snapshot from GalaxyViewModel. */
    fun postSnapshot(snapshot: RenderSnapshot) {
        renderer?.postSnapshot(snapshot)
    }

    /** Pan camera by screen-space delta (from gesture handler). */
    fun panBy(screenDx: Float, screenDy: Float) {
        camera?.panBy(screenDx, screenDy)
    }

    /** Zoom camera with focal point in screen space (from pinch gesture). */
    fun zoomBy(scaleFactor: Float, focalX: Float, focalY: Float) {
        camera?.zoomBy(scaleFactor, focalX, focalY)
    }

    /** Animate camera to a world-space target. */
    fun animateTo(worldX: Double, worldY: Double, zoom: Float) {
        camera?.animateTo(worldX, worldY, zoom)
    }

    /** Immediately snap camera to a world position and zoom. */
    fun snapTo(worldX: Double, worldY: Double, zoom: Float) {
        camera?.snapTo(worldX, worldY, zoom)
    }

    /** Convert screen coordinates to world coordinates (for hit testing). */
    fun screenToWorld(screenX: Float, screenY: Float): Pair<Double, Double>? =
        camera?.screenToWorld(screenX, screenY)

    /** Convert world coordinates to screen (for gesture feedback). */
    fun worldToScreen(worldX: Double, worldY: Double): Pair<Float, Float>? =
        camera?.worldToScreen(worldX, worldY)

    /** Current camera zoom level. */
    val currentZoom: Float get() = camera?.zoom ?: 0.01f

    /** True if camera is at surface zoom level (transition to Compose editor). */
    val isSurfaceLevel: Boolean get() = LODManager().isSurfaceLevel(currentZoom)

    // -------------------------------------------------------------------------
    // Debug
    // -------------------------------------------------------------------------

    var showDebugInfo: Boolean
        get() = renderer?.showDebugInfo ?: false
        set(value) { renderer?.showDebugInfo = value }

    // -------------------------------------------------------------------------
    // Effect triggers — forwarded to renderer
    // -------------------------------------------------------------------------

    fun triggerSupernova(bodyId: String, worldX: Double, worldY: Double, worldRadius: Double, color: Int) =
        renderer?.triggerSupernova(bodyId, worldX, worldY, worldRadius, color)

    fun triggerAccretion(bodyId: String, fromX: Double, fromY: Double, toX: Double, toY: Double, color: Int) =
        renderer?.triggerAccretion(bodyId, fromX, fromY, toX, toY, color)

    fun triggerProtostarglow(bodyId: String, worldX: Double, worldY: Double, worldRadius: Double) =
        renderer?.triggerProtostarglow(bodyId, worldX, worldY, worldRadius)

    fun triggerMoonDissolve(bodyId: String, worldX: Double, worldY: Double, parentX: Double, parentY: Double, worldRadius: Double) =
        renderer?.triggerMoonDissolve(bodyId, worldX, worldY, parentX, parentY, worldRadius)

    fun triggerWormhole(bodyId: String, worldX: Double, worldY: Double, worldRadius: Double, isSource: Boolean) =
        renderer?.triggerWormhole(bodyId, worldX, worldY, worldRadius, isSource)

    // -------------------------------------------------------------------------
    // Telescope beam
    // -------------------------------------------------------------------------

    fun setTelescopeState(active: Boolean, highlightIds: Set<String>) {
        renderer?.setTelescopeState(active, highlightIds)
    }

    fun setTelescopeCenter(screenX: Float, screenY: Float) {
        renderer?.let {
            it.telescopeCenterX = screenX
            it.telescopeCenterY = screenY
        }
    }

    // -------------------------------------------------------------------------
    // Drag line (tidal lock gesture feedback)
    // -------------------------------------------------------------------------

    fun setDragLine(
        active: Boolean,
        fromSx: Float = 0f,
        fromSy: Float = 0f,
        toSx: Float = 0f,
        toSy: Float = 0f
    ) {
        renderer?.let {
            it.dragLineActive = active
            it.dragFromSx = fromSx
            it.dragFromSy = fromSy
            it.dragToSx = toSx
            it.dragToSy = toSy
        }
    }

    // -------------------------------------------------------------------------
    // Touch / gesture routing
    // -------------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean =
        gestureRouter?.onTouchEvent(event) ?: super.onTouchEvent(event)

    /**
     * Wire up gesture callbacks. Call this from GalaxyScreen's AndroidView factory
     * after the view is created, once callbacks from the ViewModel are available.
     *
     * Must be called on the main thread. Camera may not be ready yet if surface
     * hasn't been created; GestureRouter is (re-)initialised in [surfaceChanged]
     * when the camera becomes available, so callers should re-invoke this after
     * the first [surfaceChanged] if they need the router immediately.
     */
    fun setupGestures(
        physicsWorld: PhysicsWorld,
        onBodySelected: (BodySnapshot?) -> Unit,
        onDoubleTapBody: (BodySnapshot) -> Unit,
        onDoubleTapEmpty: () -> Unit,
        onAccretionDrop: (asteroidId: String, planetId: String) -> Unit,
        onCreatePlanetFromAsteroid: (asteroidId: String, worldX: Double, worldY: Double) -> Unit,
        onCreateTidalLock: (bodyIdA: String, bodyIdB: String) -> Unit,
        onZoomChanged: (Float) -> Unit = {}
    ) {
        // Store the setup lambda so it can be re-applied after surface recreation
        val setup = {
            val cam = camera
            if (cam != null) {
                gestureRouter = GestureRouter(
                    camera = cam,
                    physicsWorld = physicsWorld,
                    onBodySelected = onBodySelected,
                    onDoubleTapBody = onDoubleTapBody,
                    onDoubleTapEmpty = onDoubleTapEmpty,
                    onAccretionDrop = onAccretionDrop,
                    onCreatePlanetFromAsteroid = onCreatePlanetFromAsteroid,
                    onCreateTidalLock = onCreateTidalLock,
                    onZoomChanged = onZoomChanged
                )
            }
        }
        pendingGestureSetup = setup
        setup() // Try immediately in case camera is already ready
    }

    /**
     * Feed the latest physics snapshot to the gesture router so hit-testing
     * uses current body positions. Call each frame from GalaxyScreen's update block.
     */
    fun updateGestureSnapshot(snapshot: PhysicsSnapshot) {
        gestureRouter?.updateSnapshot(snapshot)
    }
}
