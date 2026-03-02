package com.projectorbit.ui.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * EffectPainter manages particle effects and transient visual events.
 *
 * All Paint objects are pre-allocated. Particle pools are pre-allocated
 * to avoid GC pressure during effects.
 */
class EffectPainter {

    // --- Pre-allocated paints ---
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // -------------------------------------------------------------------------
    // Effect data classes
    // -------------------------------------------------------------------------

    enum class EffectType {
        SUPERNOVA, ACCRETION, PROTOSTAR_GLOW, MOON_DISSOLVE, WORMHOLE, TELESCOPE_BEAM
    }

    data class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var life: Float = 1f,        // 1.0 = fresh, 0.0 = dead
        var maxLife: Float = 1f,
        var radius: Float = 3f,
        var color: Int = Color.WHITE,
        var active: Boolean = false
    )

    data class ActiveEffect(
        val type: EffectType,
        val bodyId: String,
        var screenX: Float,
        var screenY: Float,
        var screenRadius: Float,
        var elapsed: Float = 0f,     // seconds since effect started
        val duration: Float,          // total duration in seconds
        val particles: Array<Particle>,
        val color: Int = Color.WHITE
    ) {
        val progress: Float get() = (elapsed / duration).coerceIn(0f, 1f)
        val isFinished: Boolean get() = elapsed >= duration
    }

    // Pool of active effects (bounded to avoid unbounded growth)
    private val activeEffects = mutableListOf<ActiveEffect>()
    private val maxEffects = 32

    // Particle pool: pre-allocated
    private val particlePool = Array(512) { Particle() }
    private var poolCursor = 0

    private fun acquireParticle(): Particle {
        val p = particlePool[poolCursor % particlePool.size]
        poolCursor++
        p.active = true
        return p
    }

    // -------------------------------------------------------------------------
    // Effect triggers
    // -------------------------------------------------------------------------

    /** Trigger a supernova effect at the given screen position. */
    fun triggerSupernova(bodyId: String, sx: Float, sy: Float, sr: Float, color: Int) {
        if (activeEffects.size >= maxEffects) return
        val particles = Array(40) { acquireParticle() }
        val rng = Random(bodyId.hashCode())
        particles.forEach { p ->
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val speed = sr * (0.8f + rng.nextFloat() * 1.2f)
            p.x = sx; p.y = sy
            p.vx = cos(angle) * speed; p.vy = sin(angle) * speed
            p.life = 1f; p.maxLife = 1f
            p.radius = 4f + rng.nextFloat() * 6f
            p.color = color
            p.active = true
        }
        activeEffects.add(ActiveEffect(EffectType.SUPERNOVA, bodyId, sx, sy, sr, duration = 1.5f, particles = particles, color = color))
    }

    /** Trigger an accretion merge effect: stream from asteroid to planet. */
    fun triggerAccretion(bodyId: String, fromSx: Float, fromSy: Float, toSx: Float, toSy: Float, planetColor: Int) {
        if (activeEffects.size >= maxEffects) return
        val particles = Array(20) { acquireParticle() }
        val dx = toSx - fromSx; val dy = toSy - fromSy
        val rng = Random(bodyId.hashCode())
        particles.forEach { p ->
            val t = rng.nextFloat()
            p.x = fromSx + dx * t + (rng.nextFloat() - 0.5f) * 20f
            p.y = fromSy + dy * t + (rng.nextFloat() - 0.5f) * 20f
            p.vx = dx * 0.6f + (rng.nextFloat() - 0.5f) * 40f
            p.vy = dy * 0.6f + (rng.nextFloat() - 0.5f) * 40f
            p.life = 0.5f + rng.nextFloat() * 0.5f; p.maxLife = p.life
            p.radius = 3f + rng.nextFloat() * 4f
            p.color = planetColor; p.active = true
        }
        val effect = ActiveEffect(EffectType.ACCRETION, bodyId, toSx, toSy, 0f, duration = 0.8f, particles = particles, color = planetColor)
        activeEffects.add(effect)
    }

    /** Trigger a protostar glow effect for a newly created note. */
    fun triggerProtostarglow(bodyId: String, sx: Float, sy: Float, sr: Float) {
        if (activeEffects.size >= maxEffects) return
        activeEffects.add(ActiveEffect(EffectType.PROTOSTAR_GLOW, bodyId, sx, sy, sr, duration = 3.0f, particles = emptyArray(), color = Color.rgb(255, 180, 60)))
    }

    /** Trigger a moon dissolve effect for sub-task completion. */
    fun triggerMoonDissolve(bodyId: String, sx: Float, sy: Float, sr: Float, parentSx: Float, parentSy: Float) {
        if (activeEffects.size >= maxEffects) return
        val particles = Array(16) { acquireParticle() }
        val dx = parentSx - sx; val dy = parentSy - sy
        val rng = Random(bodyId.hashCode())
        particles.forEach { p ->
            p.x = sx + (rng.nextFloat() - 0.5f) * sr * 2f
            p.y = sy + (rng.nextFloat() - 0.5f) * sr * 2f
            p.vx = dx * 0.4f + (rng.nextFloat() - 0.5f) * 30f
            p.vy = dy * 0.4f + (rng.nextFloat() - 0.5f) * 30f
            p.life = 0.6f + rng.nextFloat() * 0.4f; p.maxLife = p.life
            p.radius = 2f + rng.nextFloat() * 3f
            p.color = Color.rgb(180, 180, 220); p.active = true
        }
        activeEffects.add(ActiveEffect(EffectType.MOON_DISSOLVE, bodyId, sx, sy, sr, duration = 1.0f, particles = particles))
    }

    /** Trigger a wormhole vortex at a portal location. */
    fun triggerWormhole(bodyId: String, sx: Float, sy: Float, sr: Float, isSource: Boolean) {
        if (activeEffects.size >= maxEffects) return
        activeEffects.add(ActiveEffect(EffectType.WORMHOLE, bodyId, sx, sy, sr, duration = 1.2f, particles = emptyArray(), color = Color.rgb(80, 0, 180)))
    }

    /** Draw telescope beam from screen center toward search results. */
    fun triggerTelescopeBeam(sx: Float, sy: Float) {
        // Telescope beam is drawn every frame while active — no effect object needed
        // Caller manages beam state; this just provides the draw primitive below
    }

    // -------------------------------------------------------------------------
    // Update and draw
    // -------------------------------------------------------------------------

    /**
     * Update all active effects by dt seconds.
     * Call this once per render frame before draw().
     */
    fun update(dt: Float) {
        val iter = activeEffects.iterator()
        while (iter.hasNext()) {
            val effect = iter.next()
            effect.elapsed += dt
            // Update particles
            for (p in effect.particles) {
                if (!p.active) continue
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.life -= dt / p.maxLife.coerceAtLeast(0.01f) * p.maxLife
                if (p.life <= 0f) p.active = false
            }
            if (effect.isFinished) iter.remove()
        }
    }

    /**
     * Draw all active effects onto the canvas.
     */
    fun draw(canvas: Canvas) {
        for (effect in activeEffects) {
            when (effect.type) {
                EffectType.SUPERNOVA -> drawSupernova(canvas, effect)
                EffectType.ACCRETION -> drawAccretion(canvas, effect)
                EffectType.PROTOSTAR_GLOW -> drawProtostarglow(canvas, effect)
                EffectType.MOON_DISSOLVE -> drawParticles(canvas, effect)
                EffectType.WORMHOLE -> drawWormhole(canvas, effect)
                EffectType.TELESCOPE_BEAM -> { /* handled separately */ }
            }
        }
    }

    /**
     * Draw the telescope search beam. Called each frame while search is active.
     * @param canvas Canvas
     * @param centerSx Screen X of viewport center
     * @param centerSy Screen Y of viewport center
     * @param screenW Screen width
     * @param screenH Screen height
     * @param timeSec Elapsed time for animation
     */
    fun drawTelescopeBeam(
        canvas: Canvas,
        centerSx: Float, centerSy: Float,
        screenW: Float, screenH: Float,
        timeSec: Float
    ) {
        val beamLength = maxOf(screenW, screenH) * 1.5f
        val coneHalfAngle = (PI / 12).toFloat() // 15 degrees
        val rot = timeSec * 0.3f

        // Dark overlay
        beamPaint.color = Color.argb(160, 0, 0, 0)
        beamPaint.shader = null
        canvas.drawRect(0f, 0f, screenW, screenH, beamPaint)

        // Cone of light — punch through the overlay
        // (Android Canvas can't truly "erase" — approximate with bright translucent fill)
        val path = Path()
        val tipX = centerSx
        val tipY = centerSy
        val angle1 = rot - coneHalfAngle
        val angle2 = rot + coneHalfAngle
        path.moveTo(tipX, tipY)
        path.lineTo(tipX + cos(angle1) * beamLength, tipY + sin(angle1) * beamLength)
        path.lineTo(tipX + cos(angle2) * beamLength, tipY + sin(angle2) * beamLength)
        path.close()

        beamPaint.shader = RadialGradient(
            tipX, tipY, beamLength,
            intArrayOf(
                Color.argb(180, 200, 230, 255),
                Color.argb(40, 100, 150, 255),
                Color.argb(0, 50, 80, 200)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, beamPaint)
        beamPaint.shader = null
    }

    /**
     * Draw a highlight ring around a body illuminated by the telescope search.
     */
    fun drawTelescopeHighlight(canvas: Canvas, sx: Float, sy: Float, sr: Float, pulse: Float) {
        val radius = sr + 8f + pulse * 6f
        strokePaint.color = Color.argb((180 * (1f - pulse * 0.5f)).toInt(), 100, 200, 255)
        strokePaint.strokeWidth = 2.5f
        canvas.drawCircle(sx, sy, radius, strokePaint)
    }

    // -------------------------------------------------------------------------
    // Internal draw helpers
    // -------------------------------------------------------------------------

    private fun drawSupernova(canvas: Canvas, effect: ActiveEffect) {
        val p = effect.progress
        // Expanding ring
        val ringRadius = effect.screenRadius * (1f + p * 4f)
        val ringAlpha = ((1f - p) * 255).toInt()
        val r = Color.red(effect.color); val g = Color.green(effect.color); val b = Color.blue(effect.color)
        ringPaint.color = Color.argb(ringAlpha, minOf(r + 80, 255), minOf(g + 60, 255), b)
        ringPaint.strokeWidth = 3f + (1f - p) * 4f
        ringPaint.shader = null
        canvas.drawCircle(effect.screenX, effect.screenY, ringRadius, ringPaint)

        // Fading particles
        drawParticles(canvas, effect)

        // Nebula cloud forming at end
        if (p > 0.6f) {
            val cloudAlpha = ((p - 0.6f) / 0.4f * 120).toInt()
            glowPaint.shader = RadialGradient(
                effect.screenX, effect.screenY, effect.screenRadius * 3f,
                intArrayOf(
                    Color.argb(cloudAlpha, r, g, b),
                    Color.argb(0, r, g, b)
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(effect.screenX, effect.screenY, effect.screenRadius * 3f, glowPaint)
        }
    }

    private fun drawAccretion(canvas: Canvas, effect: ActiveEffect) {
        drawParticles(canvas, effect)
        // Brief flash at destination
        if (effect.progress < 0.3f) {
            val flashAlpha = ((1f - effect.progress / 0.3f) * 200).toInt()
            val r = Color.red(effect.color); val g = Color.green(effect.color); val b = Color.blue(effect.color)
            glowPaint.shader = RadialGradient(
                effect.screenX, effect.screenY, effect.screenRadius * 2f,
                intArrayOf(Color.argb(flashAlpha, 255, 255, 200), Color.argb(0, r, g, b)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(effect.screenX, effect.screenY, effect.screenRadius * 2f, glowPaint)
        }
    }

    private fun drawProtostarglow(canvas: Canvas, effect: ActiveEffect) {
        val p = effect.progress
        // Pulsing warm glow that stabilizes
        val pulse = if (p < 0.7f) sin(p * PI.toFloat() * 6f) * (1f - p / 0.7f) else 0f
        val glowRadius = effect.screenRadius * (1.8f + pulse * 0.6f)
        val alpha = ((1f - p * 0.5f) * 180).toInt()
        glowPaint.shader = RadialGradient(
            effect.screenX, effect.screenY, glowRadius,
            intArrayOf(
                Color.argb(alpha, 255, 200, 80),
                Color.argb(alpha / 2, 255, 120, 20),
                Color.argb(0, 255, 80, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(effect.screenX, effect.screenY, glowRadius, glowPaint)
    }

    private fun drawWormhole(canvas: Canvas, effect: ActiveEffect) {
        val p = effect.progress
        val cx = effect.screenX; val cy = effect.screenY
        val maxR = effect.screenRadius * 2.5f
        // Spiral vortex: draw concentric rings with rotation
        val spiralCount = 5
        for (i in 0 until spiralCount) {
            val t = i.toFloat() / spiralCount
            val r = maxR * t * (1f - p * 0.3f)
            val spiralAlpha = ((1f - t) * (1f - p) * 200).toInt()
            ringPaint.color = Color.argb(spiralAlpha, 80 + (t * 120).toInt(), 0, 200)
            ringPaint.strokeWidth = 2f
            ringPaint.shader = null
            canvas.drawCircle(cx, cy, r, ringPaint)
        }
        // Center glow
        glowPaint.shader = RadialGradient(
            cx, cy, maxR * 0.5f,
            intArrayOf(
                Color.argb(((1f - p) * 220).toInt(), 150, 0, 255),
                Color.argb(0, 80, 0, 180)
            ),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, maxR * 0.5f, glowPaint)
    }

    private fun drawParticles(canvas: Canvas, effect: ActiveEffect) {
        for (p in effect.particles) {
            if (!p.active) continue
            val lifeRatio = p.life.coerceIn(0f, 1f)
            val a = (lifeRatio * 255).toInt()
            particlePaint.color = Color.argb(a, Color.red(p.color), Color.green(p.color), Color.blue(p.color))
            particlePaint.shader = null
            canvas.drawCircle(p.x, p.y, p.radius * lifeRatio, particlePaint)
        }
    }

    /** True if there are any active effects (used to decide if extra redraws are needed). */
    fun hasActiveEffects(): Boolean = activeEffects.isNotEmpty()

    /** Remove all effects for a given body (e.g., body was removed from the world). */
    fun clearEffectsForBody(bodyId: String) {
        activeEffects.removeAll { it.bodyId == bodyId }
    }

    /** Update the screen position of an effect (body moved after effect was triggered). */
    fun updateEffectPosition(bodyId: String, sx: Float, sy: Float) {
        for (effect in activeEffects) {
            if (effect.bodyId == bodyId) {
                effect.screenX = sx
                effect.screenY = sy
            }
        }
    }
}
