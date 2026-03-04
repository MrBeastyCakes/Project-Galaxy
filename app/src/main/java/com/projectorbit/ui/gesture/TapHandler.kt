package com.projectorbit.ui.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsSnapshot
import com.projectorbit.ui.canvas.Camera

/**
 * Handles tap and double-tap gestures.
 *
 * - Single tap on body: select it, show context menu (deferred by 300ms to avoid
 *   firing on the first tap of a double-tap sequence)
 * - Single tap on empty space: deselect
 * - Double tap on body: animate camera zoom-to that body's system level
 * - Double tap on empty space: zoom out one level
 */
class TapHandler(
    private val camera: Camera,
    private val hitTester: HitTester,
    private val onBodySelected: (BodySnapshot?) -> Unit,
    private val onDoubleTapBody: (BodySnapshot) -> Unit,
    private val onDoubleTapEmpty: () -> Unit
) {

    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private val doubleTapTimeout = 300L
    private val doubleTapSlop = 50f

    private val handler = Handler(Looper.getMainLooper())
    private var pendingSingleTap: Runnable? = null

    fun onSingleTap(x: Float, y: Float, snapshot: PhysicsSnapshot) {
        val now = System.currentTimeMillis()
        val isDoubleTap = (now - lastTapTime) < doubleTapTimeout &&
            Math.abs(x - lastTapX) < doubleTapSlop &&
            Math.abs(y - lastTapY) < doubleTapSlop

        val hit = hitTester.hitTest(x, y, snapshot, camera)

        if (isDoubleTap) {
            // Cancel the pending single-tap that fired on the first tap
            pendingSingleTap?.let { handler.removeCallbacks(it) }
            pendingSingleTap = null
            // Clear state so a third tap doesn't re-trigger double-tap
            lastTapTime = 0L
            lastTapX = 0f
            lastTapY = 0f

            if (hit != null) {
                onDoubleTapBody(hit)
            } else {
                onDoubleTapEmpty()
            }
        } else {
            // Record tap position for double-tap detection on the next tap
            lastTapTime = now
            lastTapX = x
            lastTapY = y

            // Defer single-tap dispatch so a second tap can cancel it
            val runnable = Runnable {
                pendingSingleTap = null
                onBodySelected(hit)
            }
            pendingSingleTap = runnable
            handler.postDelayed(runnable, doubleTapTimeout)
        }
    }
}
