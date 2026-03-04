package com.projectorbit.ui.transition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/**
 * Zoom-driven planet-to-note morph transition.
 *
 * Instead of time-based animation, the morph progress is derived directly
 * from the camera zoom level. As the user pinch-zooms from 40→50, the planet
 * circle smoothly morphs into the full-screen M3 editor surface.
 *
 * @param morphProgress 0.0 = planet circle, 1.0 = full-screen editor. Derived from zoom.
 * @param planetColor   The planet's fill color (morph start color).
 * @param planetRadiusDp The planet's screen radius at the morph start.
 * @param content       The editor content, revealed when progress > 0.7.
 */
@Composable
fun PlanetMorphTransition(
    morphProgress: Float,
    planetColor: Color,
    planetRadiusDp: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Don't compose anything when fully collapsed
    if (morphProgress <= 0f) return

    val surfaceColor = MaterialTheme.colorScheme.surface

    // --- Derived values from progress (all instantaneous, no tween) ---

    // Size: planet diameter → full screen (eased for natural feel)
    val easedSize = easeInOutCubic(morphProgress)

    // Corner shape: circle (50%) → rounded rect (0%) — faster than size
    val cornerProgress = (morphProgress * 1.5f).coerceAtMost(1f)
    val cornerPercent = ((1f - cornerProgress) * 50f).toInt()

    // Color: planet → surface (starts at 20% progress, finishes at 80%)
    val colorProgress = ((morphProgress - 0.2f) / 0.6f).coerceIn(0f, 1f)
    val backgroundColor = lerp(planetColor, surfaceColor, colorProgress)

    // Content alpha: appears in the last 30% (progress 0.7 → 1.0)
    val contentAlpha = ((morphProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)

    // Tonal elevation: 0 → 3dp
    val elevation = (morphProgress * 3f).coerceAtMost(3f)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val planetDiameter = planetRadiusDp * 2

        // Interpolate dimensions with eased progress
        val currentW = lerp(planetDiameter, screenW, easedSize)
        val currentH = lerp(planetDiameter, screenH, easedSize)

        // Shape: percent-based corners, switching to 24.dp top corners at the end
        val shape = if (cornerPercent > 2) {
            RoundedCornerShape(cornerPercent)
        } else {
            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        }

        Surface(
            modifier = Modifier.size(currentW, currentH),
            shape = shape,
            color = backgroundColor,
            tonalElevation = elevation.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha)
            ) {
                content()
            }
        }
    }
}

/** Cubic ease-in-out for natural-feeling size interpolation. */
private fun easeInOutCubic(t: Float): Float {
    return if (t < 0.5f) {
        4f * t * t * t
    } else {
        1f - (-2f * t + 2f).let { it * it * it } / 2f
    }
}
