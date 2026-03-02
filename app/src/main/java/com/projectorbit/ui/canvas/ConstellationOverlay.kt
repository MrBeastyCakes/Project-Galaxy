package com.projectorbit.ui.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import com.projectorbit.domain.model.LinkType

/**
 * ConstellationOverlay draws backlink lines between note systems.
 *
 * Lines are drawn at galaxy/cluster zoom levels. Uses pre-allocated
 * Paint objects — no allocation in the draw loop.
 */
class ConstellationOverlay {

    // Pre-allocated paints
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.argb(80, 180, 180, 255)
        pathEffect = DashPathEffect(floatArrayOf(6f, 10f), 0f)
        strokeCap = Paint.Cap.ROUND
    }

    private val wormholePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = Color.argb(140, 160, 60, 255)
        strokeCap = Paint.Cap.ROUND
    }

    private val tidalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(100, 100, 200, 255)
        strokeCap = Paint.Cap.ROUND
    }

    /**
     * Draw all constellation/link lines.
     *
     * @param canvas      Target canvas
     * @param snapshot    Current render snapshot (contains link data)
     * @param camera      Camera for world->screen transform
     * @param lodManager  LOD manager for alpha computation
     */
    fun draw(
        canvas: Canvas,
        snapshot: RenderSnapshot,
        camera: Camera,
        lodManager: LODManager
    ) {
        val zoom = camera.zoom
        if (!lodManager.shouldDrawConstellations(zoom)) return

        val alpha = lodManager.constellationAlpha(zoom)
        if (alpha <= 0f) return

        // Build a map of body positions for fast lookup
        val posMap = HashMap<String, Pair<Float, Float>>(snapshot.bodies.size * 2)
        for (body in snapshot.bodies) {
            val (sx, sy) = camera.worldToScreen(body.positionX, body.positionY)
            posMap[body.id] = Pair(sx, sy)
        }

        for (link in snapshot.links) {
            val srcPos = posMap[link.sourceId] ?: continue
            val dstPos = posMap[link.targetId] ?: continue

            val paint = when (link.linkType) {
                LinkType.CONSTELLATION -> {
                    linePaint.alpha = (alpha * 80).toInt().coerceIn(0, 255)
                    linePaint
                }
                LinkType.WORMHOLE -> {
                    wormholePaint.alpha = (alpha * 140).toInt().coerceIn(0, 255)
                    wormholePaint
                }
                LinkType.TIDAL_LOCK -> {
                    tidalPaint.alpha = (alpha * 100).toInt().coerceIn(0, 255)
                    tidalPaint
                }
            }

            canvas.drawLine(srcPos.first, srcPos.second, dstPos.first, dstPos.second, paint)
        }
    }

    /**
     * Draw a live rubber-band line during a tidal lock drag gesture.
     * Called by the gesture layer while dragging between two planets.
     */
    fun drawDragLine(
        canvas: Canvas,
        fromSx: Float, fromSy: Float,
        toSx: Float, toSy: Float
    ) {
        tidalPaint.alpha = 180
        tidalPaint.pathEffect = null
        canvas.drawLine(fromSx, fromSy, toSx, toSy, tidalPaint)
        // Restore dash effect
        tidalPaint.pathEffect = null
    }
}
