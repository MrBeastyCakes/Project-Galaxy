package com.projectorbit.ui.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsSnapshot
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.ui.canvas.Camera

/**
 * Routes all MotionEvents to the correct handler. Only one handler is active at a time.
 *
 * State machine:
 *   IDLE -> PAN (single-finger no-body or two-finger)
 *   IDLE -> TAP (short tap on body or empty)
 *   IDLE -> DRAG (long-press on asteroid/planet)
 *
 * Single active handler pattern prevents gesture conflicts.
 */
class GestureRouter(
    private val camera: Camera,
    private val physicsWorld: PhysicsWorld,
    private val onBodySelected: (BodySnapshot?) -> Unit,
    private val onDoubleTapBody: (BodySnapshot) -> Unit,
    private val onDoubleTapEmpty: () -> Unit,
    private val onAccretionDrop: (asteroidId: String, planetId: String) -> Unit,
    private val onCreatePlanetFromAsteroid: (asteroidId: String, worldX: Double, worldY: Double) -> Unit,
    private val onCreateTidalLock: (bodyIdA: String, bodyIdB: String) -> Unit,
    private val onZoomChanged: (Float) -> Unit = {},
    private val onBodyDragged: ((bodyId: String, worldX: Double, worldY: Double) -> Unit)? = null
) {

    private enum class State { IDLE, PAN_ZOOM, TAP, DRAG }

    private var state = State.IDLE
    private val hitTester = HitTester()

    private val panZoomHandler = PanZoomHandler(camera, onZoomChanged)
    private val tapHandler = TapHandler(
        camera, hitTester, onBodySelected, onDoubleTapBody, onDoubleTapEmpty
    )
    private val dragHandler = DragHandler(
        camera, hitTester, physicsWorld,
        onAccretionDrop, onCreatePlanetFromAsteroid, onCreateTidalLock, onBodyDragged
    )

    // Long-press detection
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressTimeout = 500L
    private var pendingLongPressX = 0f
    private var pendingLongPressY = 0f
    private var hasMoved = false
    private val touchSlopSq = 20f * 20f  // 20px slop

    // Current snapshot for hit testing
    private var currentSnapshot: PhysicsSnapshot = PhysicsSnapshot.EMPTY

    fun updateSnapshot(snapshot: PhysicsSnapshot) {
        currentSnapshot = snapshot
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event)
            MotionEvent.ACTION_MOVE -> handleMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handleUp(event)
            MotionEvent.ACTION_POINTER_UP -> handlePointerUp(event)
            else -> false
        }
    }

    private fun handleDown(event: MotionEvent): Boolean {
        pendingLongPressX = event.x
        pendingLongPressY = event.y
        hasMoved = false
        state = State.TAP

        // Schedule long-press detection
        longPressHandler.postDelayed({
            if (state == State.TAP && !hasMoved) {
                state = State.DRAG
                dragHandler.onLongPress(pendingLongPressX, pendingLongPressY, currentSnapshot)
            }
        }, longPressTimeout)

        return true
    }

    private fun handlePointerDown(event: MotionEvent): Boolean {
        cancelLongPress()
        state = State.PAN_ZOOM
        panZoomHandler.onTouchEvent(event)
        return true
    }

    private fun handleMove(event: MotionEvent): Boolean {
        val dx = event.x - pendingLongPressX
        val dy = event.y - pendingLongPressY

        if (!hasMoved && dx * dx + dy * dy > touchSlopSq) {
            hasMoved = true
            if (state == State.TAP) {
                cancelLongPress()
                // Transition to pan if no body was hit at touch-down
                val hit = hitTester.hitTest(pendingLongPressX, pendingLongPressY, currentSnapshot, camera)
                state = if (hit == null) State.PAN_ZOOM else State.TAP
            }
        }

        return when (state) {
            State.PAN_ZOOM -> panZoomHandler.onTouchEvent(event)
            State.DRAG -> { dragHandler.onDragMove(event.x, event.y); true }
            State.TAP -> {
                // Moved with one finger and no body hit -- switch to pan
                if (hasMoved && event.pointerCount == 1) {
                    state = State.PAN_ZOOM
                    panZoomHandler.onTouchEvent(event)
                }
                true
            }
            State.IDLE -> false
        }
    }

    private fun handleUp(event: MotionEvent): Boolean {
        cancelLongPress()

        return when (state) {
            State.TAP -> {
                if (!hasMoved) {
                    tapHandler.onSingleTap(event.x, event.y, currentSnapshot)
                }
                state = State.IDLE
                true
            }
            State.PAN_ZOOM -> {
                panZoomHandler.onTouchEvent(event)
                state = State.IDLE
                true
            }
            State.DRAG -> {
                dragHandler.onDragEnd(event.x, event.y, currentSnapshot)
                state = State.IDLE
                true
            }
            State.IDLE -> false
        }
    }

    private fun handlePointerUp(event: MotionEvent): Boolean {
        if (event.pointerCount <= 1) {
            state = State.IDLE
        }
        return panZoomHandler.onTouchEvent(event)
    }

    private fun cancelLongPress() {
        longPressHandler.removeCallbacksAndMessages(null)
    }

    /** Called each frame to update fling deceleration. [dt] in seconds. */
    fun updateFling(dt: Float) {
        panZoomHandler.updateFling(dt)
    }

    val isDragging: Boolean get() = state == State.DRAG
    fun getDragHandler(): DragHandler = dragHandler
}
