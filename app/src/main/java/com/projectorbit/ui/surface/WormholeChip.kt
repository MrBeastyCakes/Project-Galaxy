package com.projectorbit.ui.surface

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectorbit.ui.theme.OrbitAccent

/**
 * Inline wormhole portal widget embedded in the rich text editor.
 * Tapping warps the camera to the target note's system.
 *
 * @param targetBodyId ID of the destination body
 * @param targetName display name of the destination
 * @param onTap callback to warp camera to this target
 */
@Composable
fun WormholeChip(
    targetBodyId: String,
    targetName: String,
    onTap: (targetBodyId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .border(1.dp, OrbitAccent, RoundedCornerShape(50))
            .clickable { onTap(targetBodyId) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Wormhole to $targetName",
                tint = OrbitAccent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = targetName,
                style = MaterialTheme.typography.labelSmall,
                color = OrbitAccent
            )
        }
    }
}
