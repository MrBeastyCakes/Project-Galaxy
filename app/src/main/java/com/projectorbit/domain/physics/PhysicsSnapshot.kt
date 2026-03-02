package com.projectorbit.domain.physics

import com.projectorbit.domain.model.BodyType
import com.projectorbit.util.Vec2

/**
 * Immutable snapshot of all body positions and visual states produced at the end
 * of each physics tick. Swapped into an AtomicReference for lock-free renderer consumption.
 *
 * One frame of latency (physics tick -> renderer read) is acceptable by design.
 */
data class BodySnapshot(
    val id: String,
    val positionX: Double,
    val positionY: Double,
    val radius: Double,
    val mass: Double,
    val bodyType: BodyType,
    val parentId: String?,
    val isFixed: Boolean
) {
    val position: Vec2 get() = Vec2(positionX, positionY)
}

data class PhysicsSnapshot(
    val bodies: List<BodySnapshot>,
    val tickNumber: Long,
    /** Simple integrity checksum: sum of body position hashes. Used for corruption detection on restore. */
    val checksum: Long
) {
    companion object {
        val EMPTY = PhysicsSnapshot(emptyList(), 0L, 0L)

        fun computeChecksum(bodies: List<BodySnapshot>): Long {
            var sum = 0L
            for (b in bodies) {
                sum += b.positionX.toBits()
                sum += b.positionY.toBits()
            }
            return sum
        }
    }
}
