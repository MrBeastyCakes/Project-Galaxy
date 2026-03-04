package com.projectorbit.ui.gesture

import android.view.MotionEvent
import com.projectorbit.ui.canvas.Camera
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Handles single-finger pan (no body hit) and two-finger pinch-to-zoom + pan.
 *
 * Single-finger pan: when GestureRouter confirms no body was hit, a single-finger
 * drag pans the camera by adjusting [Camera.targetCenterX/Y].
 * Two-finger pinch: adjusts [Camera.targetZoom] via pinch ratio.
 * Two-finger drag: adjusts [Camera.targetCenterX/Y].
 * Fling: applies velocity to camera center with deceleration.
 */
class PanZoomHandler(
    private val camera: Camera,
    private val onZoomChanged: (Float) -> Unit = {}
) {

    private var lastX1 = 0f
    private var lastY1 = 0f
    private var lastX2 = 0f
    private var lastY2 = 0f
    private var lastSpan = 0f

    // Fling state
    private var flingVx = 0f
    private var flingVy = 0f
    private var lastEventTime = 0L

    private var prevX = 0f
    private var prevY = 0f

    fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
                lastX1 = event.x
                lastY1 = event.y
                lastEventTime = event.eventTime
                flingVx = 0f
                flingVy = 0f
                true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    lastX1 = event.getX(0)
                    lastY1 = event.getY(0)
                    lastX2 = event.getX(1)
                    lastY2 = event.getY(1)
                    lastSpan = span(lastX1, lastY1, lastX2, lastY2)
                }
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 1) {
                    handleSingleFingerMove(event)
                } else if (pointerCount >= 2) {
                    handleTwoFingerMove(event)
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                val dt = (event.eventTime - lastEventTime).toFloat().coerceAtLeast(1f)
                val dx = event.x - prevX
                val dy = event.y - prevY
                flingVx = dx / dt * 1000f
                flingVy = dy / dt * 1000f
                true
            }

            else -> false
        }
    }

    private fun handleSingleFingerMove(event: MotionEvent) {
        val dx = event.x - lastX1
        val dy = event.y - lastY1
        camera.panBy(dx, dy)
        lastX1 = event.x
        lastY1 = event.y
        prevX = event.x
        prevY = event.y
        lastEventTime = event.eventTime
    }

    private fun handleTwoFingerMove(event: MotionEvent) {
        val x1 = event.getX(0)
        val y1 = event.getY(0)
        val x2 = event.getX(1)
        val y2 = event.getY(1)

        val currentSpan = span(x1, y1, x2, y2)
        val focalX = (x1 + x2) / 2f
        val focalY = (y1 + y2) / 2f

        // Pinch-to-zoom
        if (lastSpan > 0f && abs(currentSpan - lastSpan) > 1f) {
            val scaleFactor = currentSpan / lastSpan
            camera.zoomBy(scaleFactor, focalX, focalY)
            onZoomChanged(camera.targetZoom)
        }

        // Two-finger pan (centroid movement)
        val prevFocalX = (lastX1 + lastX2) / 2f
        val prevFocalY = (lastY1 + lastY2) / 2f
        val panDx = focalX - prevFocalX
        val panDy = focalY - prevFocalY
        if (abs(panDx) > 0.5f || abs(panDy) > 0.5f) {
            camera.panBy(panDx, panDy)
        }

        lastX1 = x1; lastY1 = y1
        lastX2 = x2; lastY2 = y2
        lastSpan = currentSpan
        lastEventTime = event.eventTime
    }

    /**
     * Update fling deceleration each frame. Call from render/update loop.
     * [dt] is delta-time in seconds.
     */
    fun updateFling(dt: Float) {
        if (abs(flingVx) < 1f && abs(flingVy) < 1f) return
        camera.panBy(flingVx * dt, flingVy * dt)
        // Frame-rate independent deceleration: 0.92 per frame at 60fps
        val decay = 0.92f.pow(dt * 60f)
        flingVx *= decay
        flingVy *= decay
    }

    private fun span(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
