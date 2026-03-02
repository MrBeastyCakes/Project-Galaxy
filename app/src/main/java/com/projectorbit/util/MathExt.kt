package com.projectorbit.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Linearly interpolate between [from] and [to] by factor [t] (unclamped). */
fun lerp(from: Double, to: Double, t: Double): Double = from + (to - from) * t

/** Linearly interpolate, clamping [t] to [0,1]. */
fun lerpClamped(from: Double, to: Double, t: Double): Double = lerp(from, to, t.coerceIn(0.0, 1.0))

fun Double.clamp(min: Double, max: Double): Double = coerceIn(min, max)

fun Double.clampMin(min: Double): Double = max(this, min)

fun Double.clampMax(max: Double): Double = min(this, max)

/** Map [value] from range [inMin]..[inMax] to range [outMin]..[outMax]. */
fun Double.remap(inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
    val t = (this - inMin) / (inMax - inMin)
    return outMin + t * (outMax - outMin)
}

/** Float variants for screen-space operations. */
fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

fun Float.clamp(min: Float, max: Float): Float = coerceIn(min, max)

/** True if the two doubles are within [epsilon] of each other. */
fun Double.nearlyEquals(other: Double, epsilon: Double = 1e-9): Boolean =
    kotlin.math.abs(this - other) < epsilon

/** Safe square root -- returns 0 for negative inputs instead of NaN. */
fun safeSqrt(value: Double): Double = if (value <= 0.0) 0.0 else sqrt(value)

/** Wrap angle to [-PI, PI]. */
fun wrapAngle(angleRad: Double): Double {
    var a = angleRad % (2 * Math.PI)
    if (a > Math.PI) a -= 2 * Math.PI
    if (a < -Math.PI) a += 2 * Math.PI
    return a
}
