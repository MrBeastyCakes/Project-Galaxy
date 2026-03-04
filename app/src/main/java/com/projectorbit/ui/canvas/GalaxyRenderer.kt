package com.projectorbit.ui.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.SurfaceHolder
import com.projectorbit.domain.model.BodyType
import kotlin.random.Random

/**
 * GalaxyRenderer drives the SurfaceView render loop at ~60fps.
 *
 * Runs on the dedicated render thread (started by GalaxySurfaceView).
 * Reads render state via a volatile reference updated by GalaxySurfaceView.
 * Reads camera state via @Volatile fields — copy-on-read at frame start.
 *
 * All paint objects are pre-allocated. No allocations in the hot draw path.
 */
class GalaxyRenderer(
    private val holder: SurfaceHolder,
    private val camera: Camera
) {

    // --- Sub-painters (all long-lived instances) ---
    private val celestialPainter = CelestialPainter()
    private val effectPainter = EffectPainter()
    private val constellationOverlay = ConstellationOverlay()
    private val lodManager = LODManager()

    // --- Background star field (pre-computed, static) ---
    private lateinit var starField: StarField

    // --- Pre-allocated background paint ---
    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    // --- Pre-allocated debug paint ---
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 200, 200, 200)
        textSize = 24f
    }

    // --- Render loop state ---
    @Volatile var running: Boolean = false
    @Volatile var showDebugInfo: Boolean = false

    // Callback fired on render thread after each camera.update(); callers must post to main thread.
    var onZoomChanged: ((Float) -> Unit)? = null

    // Latest render snapshot — set from GalaxySurfaceView (main thread)
    @Volatile private var renderSnapshot: RenderSnapshot = RenderSnapshot.EMPTY

    // Telescope beam state
    @Volatile var telescopeActive: Boolean = false
    @Volatile var telescopeCenterX: Float = 0f
    @Volatile var telescopeCenterY: Float = 0f

    private val telescopeHighlightIds = mutableSetOf<String>()
    private val highlightLock = Object()

    // Drag line state
    @Volatile var dragLineActive: Boolean = false
    @Volatile var dragFromSx: Float = 0f
    @Volatile var dragFromSy: Float = 0f
    @Volatile var dragToSx: Float = 0f
    @Volatile var dragToSy: Float = 0f

    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f

    // Timing
    private var lastFrameNanos: Long = 0L
    private var totalElapsedSec: Float = 0f
    private var fpsSmoothed: Float = 60f

    /** Initialize with surface dimensions. Call before starting render loop. */
    fun init(width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        starField = StarField(width, height, starCount = 200)
    }

    /** Update the render snapshot. Called from main thread. */
    fun postSnapshot(snapshot: RenderSnapshot) {
        renderSnapshot = snapshot
    }

    /** Main render loop — runs on the SurfaceView render thread. */
    fun renderLoop() {
        lastFrameNanos = System.nanoTime()
        while (running) {
            val nowNanos = System.nanoTime()
            val dtNanos = nowNanos - lastFrameNanos
            lastFrameNanos = nowNanos

            // Cap dt to avoid spiral-of-death on slow frames (max 50ms)
            val dt = (dtNanos / 1_000_000_000f).coerceAtMost(0.05f)
            totalElapsedSec += dt

            // Smooth FPS over ~30 frames
            val rawFps = if (dt > 0f) 1f / dt else 60f
            fpsSmoothed = fpsSmoothed * 0.95f + rawFps * 0.05f

            // Update camera smooth interpolation
            camera.update(dt)

            // Notify zoom listeners with the smoothed zoom value (render thread; callers must post to main thread)
            onZoomChanged?.invoke(camera.zoom)

            // Update particle effects
            effectPainter.update(dt)

            // Render frame
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: continue
                drawFrame(canvas, dt)
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }

            // Target 60fps: sleep if we rendered faster
            val frameNanos = System.nanoTime() - nowNanos
            val targetNanos = 16_666_666L
            val sleepNanos = targetNanos - frameNanos
            if (sleepNanos > 1_000_000L) {
                Thread.sleep(sleepNanos / 1_000_000L, (sleepNanos % 1_000_000L).toInt())
            }
        }
    }

    private fun drawFrame(canvas: Canvas, dt: Float) {
        val w = screenWidth
        val h = screenHeight

        // --- 1. Copy-on-read: capture volatile camera fields for this frame ---
        val frameZoom = camera.zoom

        // --- 2. Read latest render snapshot (volatile read — lock-free) ---
        val snapshot = renderSnapshot

        // --- 3. Draw background ---
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)
        starField.draw(canvas, camera.centerX, camera.centerY, frameZoom)

        // --- 4. Draw constellation/link lines ---
        constellationOverlay.draw(canvas, snapshot, camera, lodManager)

        // --- 5. Get LOD-filtered, culled, sorted body list ---
        val renderList = lodManager.getVisibleBodies(snapshot, camera)

        // --- 6. Draw orbit paths (faint rings) ---
        if (lodManager.shouldDrawOrbitPaths(frameZoom)) {
            val orbitAlpha = lodManager.orbitPathAlpha(frameZoom)
            drawOrbitPaths(canvas, renderList, snapshot, orbitAlpha)
        }

        // --- 7. Draw celestial bodies (sorted back-to-front by BodyType ordinal) ---
        val labelAlpha = if (lodManager.shouldDrawLabels(frameZoom)) lodManager.labelAlpha(frameZoom) else 0f
        for (entry in renderList) {
            val body = entry.body
            val (sx, sy) = camera.worldToScreen(body.positionX, body.positionY)
            val rawSr = camera.worldRadiusToScreen(body.radius)
            val sr = rawSr.coerceIn(4f, maxScreenRadius(body.bodyType, frameZoom))
            celestialPainter.draw(
                canvas, body, sx, sy, sr,
                alpha = entry.alpha,
                zoom = frameZoom,
                timeSec = totalElapsedSec,
                labelAlpha = labelAlpha
            )
        }

        // --- 8. Draw particle effects ---
        effectPainter.draw(canvas)

        // --- 9. Draw telescope beam overlay ---
        if (telescopeActive) {
            effectPainter.drawTelescopeBeam(
                canvas,
                telescopeCenterX, telescopeCenterY,
                w, h,
                totalElapsedSec
            )
            val pulse = (totalElapsedSec * 3f) % 1f
            synchronized(highlightLock) {
                for (entry in renderList) {
                    if (entry.body.id in telescopeHighlightIds) {
                        val (sx, sy) = camera.worldToScreen(entry.body.positionX, entry.body.positionY)
                        val sr = camera.worldRadiusToScreen(entry.body.radius).coerceAtLeast(4f)
                        effectPainter.drawTelescopeHighlight(canvas, sx, sy, sr, pulse)
                    }
                }
            }
        }

        // --- 10. Draw drag line for tidal lock gesture ---
        if (dragLineActive) {
            constellationOverlay.drawDragLine(canvas, dragFromSx, dragFromSy, dragToSx, dragToSy)
        }

        // --- 11. Debug overlay ---
        if (showDebugInfo) {
            canvas.drawText(
                "FPS: ${fpsSmoothed.toInt()}  Bodies: ${snapshot.bodies.size}  Zoom: ${"%.3f".format(frameZoom)}  LOD: ${camera.currentLodName()}",
                16f, 40f, debugPaint
            )
        }
    }

    private fun maxScreenRadius(type: BodyType, zoom: Float): Float = when (type) {
        BodyType.SUN -> 80f
        BodyType.BINARY_STAR -> 80f
        BodyType.GAS_GIANT -> 60f
        BodyType.PLANET, BodyType.DWARF_PLANET -> if (zoom > Camera.ZOOM_PLANET_MIN) 120f else 48f
        BodyType.MOON -> 24f
        BodyType.ASTEROID -> 16f
        BodyType.NEBULA -> 40f
    }

    private fun drawOrbitPaths(
        canvas: Canvas,
        renderList: List<LODManager.RenderEntry>,
        snapshot: RenderSnapshot,
        alpha: Float
    ) {
        val posMap = HashMap<String, Pair<Float, Float>>(snapshot.bodies.size * 2)
        for (body in snapshot.bodies) {
            val (sx, sy) = camera.worldToScreen(body.positionX, body.positionY)
            posMap[body.id] = Pair(sx, sy)
        }

        for (entry in renderList) {
            val body = entry.body
            val parentId = body.parentId ?: continue
            val parentPos = posMap[parentId] ?: continue
            val orbitRadiusScreen = camera.worldRadiusToScreen(body.orbitRadius)
            if (orbitRadiusScreen < 2f) continue
            celestialPainter.drawOrbitPath(
                canvas,
                parentPos.first, parentPos.second,
                orbitRadiusScreen,
                alpha * entry.alpha
            )
        }
    }

    // -------------------------------------------------------------------------
    // Telescope state management (called from main thread)
    // -------------------------------------------------------------------------

    fun setTelescopeState(active: Boolean, highlightIds: Set<String>) {
        telescopeActive = active
        synchronized(highlightLock) {
            telescopeHighlightIds.clear()
            telescopeHighlightIds.addAll(highlightIds)
        }
    }

    // -------------------------------------------------------------------------
    // Effect triggers (called from main thread via GalaxySurfaceView)
    // -------------------------------------------------------------------------

    fun triggerSupernova(bodyId: String, worldX: Double, worldY: Double, worldRadius: Double, color: Int) {
        val (sx, sy) = camera.worldToScreen(worldX, worldY)
        val sr = camera.worldRadiusToScreen(worldRadius)
        effectPainter.triggerSupernova(bodyId, sx, sy, sr, color)
    }

    fun triggerAccretion(bodyId: String, fromWorldX: Double, fromWorldY: Double, toWorldX: Double, toWorldY: Double, planetColor: Int) {
        val (fsx, fsy) = camera.worldToScreen(fromWorldX, fromWorldY)
        val (tsx, tsy) = camera.worldToScreen(toWorldX, toWorldY)
        effectPainter.triggerAccretion(bodyId, fsx, fsy, tsx, tsy, planetColor)
    }

    fun triggerProtostarglow(bodyId: String, worldX: Double, worldY: Double, worldRadius: Double) {
        val (sx, sy) = camera.worldToScreen(worldX, worldY)
        val sr = camera.worldRadiusToScreen(worldRadius)
        effectPainter.triggerProtostarglow(bodyId, sx, sy, sr)
    }

    fun triggerMoonDissolve(bodyId: String, worldX: Double, worldY: Double, parentWorldX: Double, parentWorldY: Double, worldRadius: Double) {
        val (sx, sy) = camera.worldToScreen(worldX, worldY)
        val (psx, psy) = camera.worldToScreen(parentWorldX, parentWorldY)
        val sr = camera.worldRadiusToScreen(worldRadius)
        effectPainter.triggerMoonDissolve(bodyId, sx, sy, sr, psx, psy)
    }

    fun triggerWormhole(bodyId: String, worldX: Double, worldY: Double, worldRadius: Double, isSource: Boolean) {
        val (sx, sy) = camera.worldToScreen(worldX, worldY)
        val sr = camera.worldRadiusToScreen(worldRadius)
        effectPainter.triggerWormhole(bodyId, sx, sy, sr, isSource)
    }

    // -------------------------------------------------------------------------
    // Inner class: static star field background
    // -------------------------------------------------------------------------

    private class StarField(width: Int, height: Int, starCount: Int) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Stars stored as (x, y, radius, alpha, parallaxFactor)
        private val stars = Array(starCount) { FloatArray(5) }

        init {
            val rng = Random(42) // fixed seed for reproducibility
            for (i in 0 until starCount) {
                stars[i][0] = rng.nextFloat() * width
                stars[i][1] = rng.nextFloat() * height
                stars[i][2] = 0.5f + rng.nextFloat() * 1.5f
                stars[i][3] = 100f + rng.nextFloat() * 155f
                stars[i][4] = rng.nextFloat() * 0.05f
            }
        }

        fun draw(canvas: Canvas, cameraCx: Double, cameraCy: Double, zoom: Float) {
            val cw = canvas.width.toFloat()
            val ch = canvas.height.toFloat()
            val parallaxScale = zoom * 0.1f
            for (star in stars) {
                val offsetX = (cameraCx * star[4] * parallaxScale).toFloat()
                val offsetY = (cameraCy * star[4] * parallaxScale).toFloat()
                val starRadius = star[2]
                paint.color = Color.argb(star[3].toInt(), 255, 255, 255)
                // Tile-based wrapping: render each star across a 3x3 grid of tiles.
                // Only tiles that overlap the screen are drawn, eliminating modulo snap.
                for (tileX in -1..1) {
                    for (tileY in -1..1) {
                        val sx = star[0] + offsetX + tileX * cw
                        val sy = star[1] + offsetY + tileY * ch
                        if (sx >= -starRadius && sx <= cw + starRadius &&
                            sy >= -starRadius && sy <= ch + starRadius) {
                            canvas.drawCircle(sx, sy, starRadius, paint)
                        }
                    }
                }
            }
        }
    }
}
