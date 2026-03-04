package com.projectorbit.ui.contextmenu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectorbit.domain.model.BodyType
import com.projectorbit.ui.theme.DeleteRed
import com.projectorbit.ui.theme.MenuBackground
import com.projectorbit.ui.theme.MenuBorder
import com.projectorbit.ui.theme.OrbitAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Radial context menu displayed around the selected celestial body.
 *
 * Positioned using screen coordinates derived from Camera.worldToScreen().
 * Actions are laid out in a circle around the body center.
 *
 * @param screenX body center X in screen pixels
 * @param screenY body center Y in screen pixels
 * @param bodyType type of selected body
 * @param isPinned whether the body is pinned
 * @param isShared whether the body is shared
 * @param onAction callback when action is selected
 * @param onDismiss callback when tapping outside
 */
@Composable
fun BodyContextMenu(
    screenX: Float,
    screenY: Float,
    bodyType: BodyType,
    isPinned: Boolean,
    isShared: Boolean,
    onAction: (MenuAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = actionsForBodyType(bodyType, isPinned, isShared)
    val count = actions.size
    val menuRadiusDp = 90.dp
    val buttonSizeDp = 64.dp
    val density = LocalDensity.current
    val menuRadiusPx = with(density) { menuRadiusDp.toPx() }
    val buttonHalfPx = with(density) { (buttonSizeDp / 2).toPx() }.roundToInt()

    BoxWithConstraints(modifier = modifier) {
        val screenWidthPx = constraints.maxWidth
        val screenHeightPx = constraints.maxHeight
        val buttonSizePx = with(density) { buttonSizeDp.toPx() }.roundToInt()

        // Scrim — tapping outside dismisses
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onDismiss)
        )

        // Radial action buttons
        actions.forEachIndexed { index, action ->
            val angle = (2.0 * PI * index / count) - PI / 2.0
            val offsetX = (cos(angle) * menuRadiusPx).roundToInt()
            val offsetY = (sin(angle) * menuRadiusPx).roundToInt()

            // Clamp to screen bounds so buttons near edges are not clipped
            val rawX = screenX.roundToInt() + offsetX - buttonHalfPx
            val rawY = screenY.roundToInt() + offsetY - buttonHalfPx
            val clampedX = rawX.coerceIn(0, (screenWidthPx - buttonSizePx).coerceAtLeast(0))
            val clampedY = rawY.coerceIn(0, (screenHeightPx - buttonSizePx).coerceAtLeast(0))

            val iconColor = if (action.isDestructive) DeleteRed else OrbitAccent

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset {
                        IntOffset(x = clampedX, y = clampedY)
                    }
                    .size(buttonSizeDp)
                    .clip(CircleShape)
                    .background(MenuBackground)
                    .border(1.dp, MenuBorder, CircleShape)
                    .clickable { onAction(action) }
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = action.label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}
