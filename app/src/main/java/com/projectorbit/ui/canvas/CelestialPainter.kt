package com.projectorbit.ui.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.projectorbit.domain.model.BodyType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * CelestialPainter handles all draw routines for celestial body types.
 *
 * All Paint objects are pre-allocated and reused per frame — NO allocation in the draw loop.
 * Instance is created once and retained for the lifetime of the renderer.
 */
class CelestialPainter {

    // --- Pre-allocated Paint objects (never allocate inside draw methods) ---

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val atmospherePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(60, 255, 255, 255)
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Reusable path for asteroid diamond shape
    private val asteroidPath = Path()

    // -------------------------------------------------------------------------
    // Public draw dispatch
    // -------------------------------------------------------------------------

    /**
     * Draw a single celestial body. Called for each body in the render list.
     *
     * @param canvas     Target canvas (already in screen space)
     * @param body       RenderBody with physics + visual fields
     * @param sx         Screen X (pre-computed from camera transform)
     * @param sy         Screen Y (pre-computed)
     * @param sr         Screen radius (pre-computed)
     * @param alpha      LOD fade alpha [0,1]
     * @param zoom       Current camera zoom
     * @param timeSec    Elapsed time in seconds (for animation)
     * @param labelAlpha Alpha for name label (0 = hidden)
     */
    fun draw(
        canvas: Canvas,
        body: RenderBody,
        sx: Float,
        sy: Float,
        sr: Float,
        alpha: Float,
        zoom: Float,
        timeSec: Float,
        labelAlpha: Float
    ) {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        when (body.bodyType) {
            BodyType.SUN -> drawSun(canvas, body, sx, sy, sr, a, zoom, timeSec)
            BodyType.GAS_GIANT -> drawGasGiant(canvas, body, sx, sy, sr, a, timeSec)
            BodyType.BINARY_STAR -> drawBinaryStar(canvas, body, sx, sy, sr, a, timeSec)
            BodyType.PLANET -> drawPlanet(canvas, body, sx, sy, sr, a, zoom)
            BodyType.DWARF_PLANET -> drawDwarfPlanet(canvas, body, sx, sy, sr, a)
            BodyType.MOON -> drawMoon(canvas, body, sx, sy, sr, a)
            BodyType.ASTEROID -> drawAsteroid(canvas, body, sx, sy, sr, a, timeSec)
            BodyType.NEBULA -> drawNebula(canvas, body, sx, sy, sr, a, timeSec)
        }

        if (labelAlpha > 0f && body.name.isNotEmpty()) {
            drawLabel(canvas, body.name, sx, sy, sr, labelAlpha)
        }

        if (body.isShared && body.bodyType == BodyType.PLANET) {
            drawRingSystem(canvas, sx, sy, sr, a)
        }
    }

    // -------------------------------------------------------------------------
    // Per-type draw routines
    // -------------------------------------------------------------------------

    private fun drawSun(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int,
        @Suppress("UNUSED_PARAMETER") zoom: Float,
        timeSec: Float
    ) {
        // Outer glow (larger, more transparent)
        val glowRadius = sr * 2.5f
        glowPaint.shader = RadialGradient(
            sx, sy, glowRadius,
            intArrayOf(
                Color.argb((alpha * 0.6f).toInt(), 255, 240, 100),
                Color.argb((alpha * 0.2f).toInt(), 255, 140, 0),
                Color.argb(0, 255, 80, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sx, sy, glowRadius, glowPaint)

        // Core radial gradient: white -> yellow -> orange
        basePaint.shader = RadialGradient(
            sx, sy, sr,
            intArrayOf(
                Color.argb(alpha, 255, 255, 240),
                Color.argb(alpha, 255, 220, 50),
                Color.argb(alpha, 255, 120, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sx, sy, sr, basePaint)

        // Animated corona flicker (always drawn when sun is visible)
        drawCorona(canvas, sx, sy, sr, alpha, timeSec)
    }

    private fun drawCorona(
        canvas: Canvas,
        sx: Float, sy: Float, sr: Float,
        alpha: Int,
        timeSec: Float
    ) {
        val spikes = 8
        strokePaint.color = Color.argb((alpha * 0.5f).toInt(), 255, 200, 50)
        strokePaint.strokeWidth = 1.5f
        strokePaint.shader = null
        for (i in 0 until spikes) {
            val angle = (2.0 * PI * i / spikes + timeSec * 0.3).toFloat()
            val len = sr * (1.4f + 0.2f * sin((timeSec * 2f + i).toDouble()).toFloat())
            val startX = sx + cos(angle) * sr
            val startY = sy + sin(angle) * sr
            val endX = sx + cos(angle) * len
            val endY = sy + sin(angle) * len
            canvas.drawLine(startX, startY, endX, endY, strokePaint)
        }
    }

    private fun drawGasGiant(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int,
        timeSec: Float
    ) {
        val baseColor = body.color
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)
        basePaint.shader = RadialGradient(
            sx, sy - sr * 0.2f, sr,
            intArrayOf(
                Color.argb(alpha, minOf(r + 60, 255), minOf(g + 40, 255), minOf(b + 40, 255)),
                Color.argb(alpha, r, g, b),
                Color.argb(alpha, maxOf(r - 40, 0), maxOf(g - 20, 0), maxOf(b - 20, 0))
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sx, sy, sr, basePaint)

        // Subtle glow
        glowPaint.shader = RadialGradient(
            sx, sy, sr * 1.8f,
            intArrayOf(
                Color.argb((alpha * 0.3f).toInt(), r, g, b),
                Color.argb(0, r, g, b)
            ),
            floatArrayOf(0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sx, sy, sr * 1.8f, glowPaint)
    }

    private fun drawBinaryStar(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int,
        timeSec: Float
    ) {
        val offset = sr * 0.5f
        val angle = timeSec * 0.5f

        val x1 = sx + cos(angle) * offset
        val y1 = sy + sin(angle) * offset
        val x2 = sx - cos(angle) * offset
        val y2 = sy - sin(angle) * offset

        // Star 1: warm yellow
        glowPaint.shader = RadialGradient(
            x1, y1, sr,
            intArrayOf(
                Color.argb(alpha, 255, 240, 150),
                Color.argb((alpha * 0.5f).toInt(), 255, 200, 50),
                Color.argb(0, 255, 100, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x1, y1, sr, glowPaint)

        // Star 2: cool blue-white
        glowPaint.shader = RadialGradient(
            x2, y2, sr,
            intArrayOf(
                Color.argb(alpha, 200, 220, 255),
                Color.argb((alpha * 0.5f).toInt(), 100, 150, 255),
                Color.argb(0, 50, 80, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x2, y2, sr, glowPaint)
    }

    private fun drawPlanet(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int,
        zoom: Float
    ) {
        val baseColor = body.color
        basePaint.shader = null
        basePaint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
        canvas.drawCircle(sx, sy, sr, basePaint)

        // Atmosphere ring (tag-colored gradient) if body has atmosphere
        if (body.atmosphereColor != 0 && zoom > Camera.ZOOM_CLUSTER_MAX) {
            val atmosphereRadius = sr * 1.35f
            val ar = Color.red(body.atmosphereColor)
            val ag = Color.green(body.atmosphereColor)
            val ab = Color.blue(body.atmosphereColor)
            val density = body.atmosphereDensity.toFloat().coerceIn(0f, 1f)
            atmospherePaint.shader = RadialGradient(
                sx, sy, atmosphereRadius,
                intArrayOf(
                    Color.argb(0, ar, ag, ab),
                    Color.argb((alpha * density * 0.6f).toInt(), ar, ag, ab),
                    Color.argb((alpha * density * 0.8f).toInt(), ar, ag, ab),
                    Color.argb(0, ar, ag, ab)
                ),
                floatArrayOf(0.6f, 0.75f, 0.88f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(sx, sy, atmosphereRadius, atmospherePaint)
        }
    }

    private fun drawDwarfPlanet(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int
    ) {
        // Fill with a subtle radial gradient
        basePaint.shader = RadialGradient(
            sx, sy, sr,
            intArrayOf(
                Color.argb(alpha, 180, 200, 220),
                Color.argb(alpha, 120, 140, 170),
                Color.argb(alpha, 80, 100, 130)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sx, sy, sr * 0.85f, basePaint)

        // Dashed outline to distinguish from regular planets
        strokePaint.shader = null
        strokePaint.color = Color.argb(alpha, 160, 180, 200)
        strokePaint.strokeWidth = 2f
        strokePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
        canvas.drawCircle(sx, sy, sr * 0.85f, strokePaint)
        strokePaint.pathEffect = null
    }

    private fun drawMoon(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int
    ) {
        val moonRadius = sr.coerceAtLeast(3f)
        // Moons draw as outlined circles (stroke only) to distinguish from planets
        strokePaint.shader = null
        strokePaint.color = Color.argb(alpha, 200, 200, 210)
        strokePaint.strokeWidth = 2f
        strokePaint.pathEffect = null
        canvas.drawCircle(sx, sy, moonRadius, strokePaint)

        if (body.isCompleted) {
            strokePaint.color = Color.argb((alpha * 0.8f).toInt(), 100, 220, 100)
            strokePaint.strokeWidth = 1.5f
            canvas.drawCircle(sx, sy, moonRadius + 2f, strokePaint)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun drawAsteroid(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int,
        timeSec: Float
    ) {
        val radius = sr.coerceAtLeast(2f)

        // Draw as a diamond (rotated square) for clear visual distinction
        asteroidPath.reset()
        asteroidPath.moveTo(sx, sy - radius)   // top
        asteroidPath.lineTo(sx + radius, sy)   // right
        asteroidPath.lineTo(sx, sy + radius)   // bottom
        asteroidPath.lineTo(sx - radius, sy)   // left
        asteroidPath.close()

        basePaint.shader = null
        basePaint.style = Paint.Style.FILL
        basePaint.color = Color.argb(alpha, 160, 150, 140)
        canvas.drawPath(asteroidPath, basePaint)
    }

    private fun drawNebula(
        canvas: Canvas,
        body: RenderBody,
        sx: Float, sy: Float, sr: Float,
        alpha: Int,
        timeSec: Float
    ) {
        val nebulaRadius = sr * 2.0f
        val baseColor = body.color.takeIf { it != 0 } ?: Color.rgb(120, 80, 180)
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)

        val driftX = sx + sin(timeSec * 0.1).toFloat() * sr * 0.2f
        val driftY = sy + cos(timeSec * 0.13).toFloat() * sr * 0.2f

        atmospherePaint.shader = RadialGradient(
            driftX, driftY, nebulaRadius,
            intArrayOf(
                Color.argb((alpha * 0.5f).toInt(), r, g, b),
                Color.argb((alpha * 0.25f).toInt(), r, g, b),
                Color.argb(0, r, g, b)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(driftX, driftY, nebulaRadius, atmospherePaint)
    }

    private fun drawRingSystem(
        canvas: Canvas,
        sx: Float, sy: Float, sr: Float,
        alpha: Int
    ) {
        val rings = 3
        for (i in 1..rings) {
            val ringA = sr * (1.4f + i * 0.25f)
            val ringB = ringA * 0.35f
            val ringAlpha = (alpha * (0.6f - i * 0.15f)).toInt().coerceIn(0, 255)
            ringPaint.color = Color.argb(ringAlpha, 180, 160, 120)
            ringPaint.strokeWidth = 1.5f
            ringPaint.shader = null
            canvas.drawOval(
                sx - ringA, sy - ringB,
                sx + ringA, sy + ringB,
                ringPaint
            )
        }
    }

    private fun drawLabel(
        canvas: Canvas,
        name: String,
        sx: Float, sy: Float, sr: Float,
        labelAlpha: Float
    ) {
        labelPaint.alpha = (labelAlpha * 255).toInt().coerceIn(0, 255)
        canvas.drawText(name, sx, sy + sr + labelPaint.textSize + 4f, labelPaint)
    }

    /**
     * Draw a faint dotted orbit path ring around the parent position.
     * Called for planets/moons when orbit paths are enabled.
     */
    fun drawOrbitPath(
        canvas: Canvas,
        parentSx: Float, parentSy: Float,
        orbitRadiusScreen: Float,
        alpha: Float
    ) {
        orbitPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawCircle(parentSx, parentSy, orbitRadiusScreen, orbitPaint)
    }
}
