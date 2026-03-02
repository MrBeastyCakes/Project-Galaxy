package com.projectorbit.ui.gesture

import android.view.MotionEvent
import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsSnapshot
import com.projectorbit.ui.canvas.Camera

/**
 * Handles tap and double-tap gestures.
 *
 * - Single tap on body: select it, show context menu
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

    fun onSingleTap(x: Float, y: Float, snapshot: PhysicsSnapshot) {
        val now = System.currentTimeMillis()
        val isDoubleTap = (now - lastTapTime) < doubleTapTimeout &&
            Math.abs(x - lastTapX) < doubleTapSlop &&
            Math.abs(y - lastTapY) < doubleTapSlop

        lastTapTime = now
        lastTapX = x
        lastTapY = y

        val hit = hitTester.hitTest(x, y, snapshot, camera)

        if (isDoubleTap) {
            if (hit != null) {
                onDoubleTapBody(hit)
            } else {
                onDoubleTapEmpty()
            }
        } else {
            onBodySelected(hit)
        }
    }
}
