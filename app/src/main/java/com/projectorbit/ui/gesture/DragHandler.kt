package com.projectorbit.ui.gesture

import android.view.MotionEvent
import com.projectorbit.domain.model.BodyType
import com.projectorbit.domain.physics.BodySnapshot
import com.projectorbit.domain.physics.PhysicsSnapshot
import com.projectorbit.domain.physics.PhysicsWorld
import com.projectorbit.ui.canvas.Camera
import com.projectorbit.util.Vec2

/**
 * Handles long-press drag interactions:
 * - Long-press on asteroid: enter drag mode, asteroid follows finger
 *   - Drop on planet: trigger accretion merge
 *   - Drop on empty space: create new planet from asteroid
 * - Long-press on planet: enter drag mode, planet follows finger
 *   - On drop: update orbit position/radius relative to parent Sun
 * - Long-press on Sun: drag to reposition the Sun
 */
class DragHandler(
    private val camera: Camera,
    private val hitTester: HitTester,
    private val physicsWorld: PhysicsWorld,
    private val onAccretionDrop: (asteroidId: String, planetId: String) -> Unit,
    private val onCreatePlanetFromAsteroid: (asteroidId: String, worldX: Double, worldY: Double) -> Unit,
    private val onCreateTidalLock: (bodyIdA: String, bodyIdB: String) -> Unit,
    private val onBodyDragged: ((bodyId: String, worldX: Double, worldY: Double) -> Unit)? = null
) {

    enum class DragMode { NONE, ASTEROID_DRAG, BODY_DRAG }

    var dragMode = DragMode.NONE
        private set
    var draggedBodyId: String? = null
        private set

    // Current finger world position for rubber-band rendering
    @Volatile var dragWorldX: Double = 0.0
    @Volatile var dragWorldY: Double = 0.0

    fun onLongPress(x: Float, y: Float, snapshot: PhysicsSnapshot) {
        val hit = hitTester.hitTest(x, y, snapshot, camera) ?: return
        val (wx, wy) = camera.screenToWorld(x, y)

        when (hit.bodyType) {
            BodyType.ASTEROID -> {
                dragMode = DragMode.ASTEROID_DRAG
                draggedBodyId = hit.id
                dragWorldX = wx
                dragWorldY = wy
            }
            BodyType.PLANET, BodyType.DWARF_PLANET, BodyType.SUN, BodyType.GAS_GIANT -> {
                dragMode = DragMode.BODY_DRAG
                draggedBodyId = hit.id
                dragWorldX = wx
                dragWorldY = wy
            }
            else -> { /* no drag for other types */ }
        }
    }

    fun onDragMove(x: Float, y: Float) {
        val (wx, wy) = camera.screenToWorld(x, y)
        dragWorldX = wx
        dragWorldY = wy

        val id = draggedBodyId ?: return
        when (dragMode) {
            DragMode.ASTEROID_DRAG, DragMode.BODY_DRAG -> {
                physicsWorld.moveBody(id, Vec2(wx, wy))
            }
            DragMode.NONE -> { /* nothing */ }
        }
    }

    fun onDragEnd(x: Float, y: Float, snapshot: PhysicsSnapshot) {
        val (wx, wy) = camera.screenToWorld(x, y)
        val dropHit = hitTester.hitTest(x, y, snapshot, camera)

        when (dragMode) {
            DragMode.ASTEROID_DRAG -> {
                val asteroidId = draggedBodyId ?: return reset()
                if (dropHit != null && dropHit.id != asteroidId &&
                    (dropHit.bodyType == BodyType.PLANET || dropHit.bodyType == BodyType.GAS_GIANT)
                ) {
                    onAccretionDrop(asteroidId, dropHit.id)
                } else {
                    onCreatePlanetFromAsteroid(asteroidId, wx, wy)
                }
            }
            DragMode.BODY_DRAG -> {
                val bodyId = draggedBodyId ?: return reset()
                // Notify ViewModel to persist new position and update orbit radius
                onBodyDragged?.invoke(bodyId, wx, wy)
            }
            DragMode.NONE -> { /* nothing */ }
        }

        reset()
    }

    fun onDragCancel() = reset()

    private fun reset() {
        dragMode = DragMode.NONE
        draggedBodyId = null
    }
}
