package com.projectorbit.util

import kotlin.math.sqrt

/**
 * Immutable 2D vector using Double precision for world-space coordinates.
 * All physics and world-space operations use Double to prevent precision loss
 * at large canvas extents (>8192 units from origin).
 */
data class Vec2(val x: Double = 0.0, val y: Double = 0.0) {

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    operator fun times(scalar: Double): Vec2 = Vec2(x * scalar, y * scalar)

    operator fun div(scalar: Double): Vec2 = Vec2(x / scalar, y / scalar)

    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    fun dot(other: Vec2): Double = x * other.x + y * other.y

    fun lengthSquared(): Double = x * x + y * y

    fun length(): Double = sqrt(lengthSquared())

    fun normalized(): Vec2 {
        val len = length()
        return if (len < 1e-12) Vec2.ZERO else Vec2(x / len, y / len)
    }

    fun distanceTo(other: Vec2): Double = (other - this).length()

    fun distanceSquaredTo(other: Vec2): Double = (other - this).lengthSquared()

    /** Rotate 90 degrees counter-clockwise (perpendicular vector). */
    fun perpendicular(): Vec2 = Vec2(-y, x)

    fun lerp(target: Vec2, t: Double): Vec2 = Vec2(
        x + (target.x - x) * t,
        y + (target.y - y) * t
    )

    fun isNaN(): Boolean = x.isNaN() || y.isNaN()

    fun isFinite(): Boolean = x.isFinite() && y.isFinite()

    companion object {
        val ZERO = Vec2(0.0, 0.0)
        val ONE = Vec2(1.0, 1.0)
    }
}
