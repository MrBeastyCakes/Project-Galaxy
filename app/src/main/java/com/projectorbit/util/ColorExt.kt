package com.projectorbit.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

/**
 * Blend two ARGB packed ints additively, clamping each channel to [0,255].
 */
fun blendColorsAdditive(colorA: Int, colorB: Int, alphaB: Float): Int {
    val rA = (colorA shr 16) and 0xFF
    val gA = (colorA shr 8) and 0xFF
    val bA = colorA and 0xFF
    val aA = (colorA shr 24) and 0xFF

    val rB = (colorB shr 16) and 0xFF
    val gB = (colorB shr 8) and 0xFF
    val bB = colorB and 0xFF

    val t = alphaB.coerceIn(0f, 1f)
    val r = (rA + rB * t).roundToInt().coerceIn(0, 255)
    val g = (gA + gB * t).roundToInt().coerceIn(0, 255)
    val b = (bA + bB * t).roundToInt().coerceIn(0, 255)

    return (aA shl 24) or (r shl 16) or (g shl 8) or b
}

/**
 * Linearly interpolate between two ARGB packed ints by [t] in [0,1].
 */
fun lerpColor(from: Int, to: Int, t: Float): Int {
    val tf = t.coerceIn(0f, 1f)
    val aF = (from shr 24) and 0xFF
    val rF = (from shr 16) and 0xFF
    val gF = (from shr 8) and 0xFF
    val bF = from and 0xFF

    val aT = (to shr 24) and 0xFF
    val rT = (to shr 16) and 0xFF
    val gT = (to shr 8) and 0xFF
    val bT = to and 0xFF

    val a = lerp(aF.toFloat(), aT.toFloat(), tf).roundToInt()
    val r = lerp(rF.toFloat(), rT.toFloat(), tf).roundToInt()
    val g = lerp(gF.toFloat(), gT.toFloat(), tf).roundToInt()
    val b = lerp(bF.toFloat(), bT.toFloat(), tf).roundToInt()

    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/**
 * Default atmosphere color for a given tag name.
 * Returns ARGB packed int.
 */
fun atmosphereColorForTag(tagName: String): Int = when (tagName.lowercase()) {
    "urgent" -> 0xFFFF4444.toInt()
    "reference" -> 0xFF4488FF.toInt()
    "in progress" -> 0xFFFFAA00.toInt()
    "done" -> 0xFF44DD44.toInt()
    "idea" -> 0xFFDD44FF.toInt()
    else -> 0xFF888888.toInt()
}

/**
 * Convert a Compose [Color] to an ARGB packed int.
 */
fun Color.toArgbInt(): Int = this.toArgb()

/**
 * Blend multiple atmosphere colors additively. Each entry is (argbColor, density).
 * Returns the blended ARGB packed int. Returns transparent black if list is empty.
 */
fun blendAtmospheres(atmospheres: List<Pair<Int, Double>>): Int {
    if (atmospheres.isEmpty()) return 0x00000000
    var result = 0xFF000000.toInt()
    for ((color, density) in atmospheres) {
        result = blendColorsAdditive(result, color, density.toFloat())
    }
    return result
}
