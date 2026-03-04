package com.projectorbit.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TelescopeOverlay is the Compose search UI layer.
 *
 * It renders a darkened overlay with a search field, result count, and tappable result chips.
 * The actual telescope beam drawing (cone of light, body highlights) is handled
 * by GalaxyRenderer / EffectPainter on the SurfaceView layer beneath this overlay.
 *
 * @param query              Current search query string
 * @param onQueryChange      Called when query changes
 * @param resultCount        Number of matching bodies
 * @param matchedBodies      List of (bodyId, bodyName) pairs for result chips
 * @param onDismiss          Called when user closes the telescope view
 * @param onNavigateToResult Called with the bodyId when user taps a result chip
 */
@Composable
fun TelescopeOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    matchedBodies: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateToResult: (String) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    // Pulse animation for the search icon
    val infiniteTransition = rememberInfiniteTransition(label = "telescope_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Dark background overlay (the beam is drawn by the SurfaceView beneath)
            .background(Color(0x99000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // --- Search bar ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF1A1A2E),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 8.dp)
            ) {
                // Telescope icon (pulsing)
                Text(
                    text = "\uD83D\uDD2D", // telescope emoji fallback
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = pulseAlpha),
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                )

                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            "Search notes...",
                            color = Color(0xFF6B7A99),
                            fontSize = 16.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { /* results already live */ }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF7A9FFF),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )

                // Dismiss button
                IconButton(onClick = onDismiss) {
                    Text(
                        text = "\u2715", // X
                        color = Color(0xFF8899BB),
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Result count / status line ---
            if (query.isNotEmpty()) {
                Text(
                    text = when {
                        resultCount == 0 -> "No matching notes found"
                        resultCount == 1 -> "1 note illuminated"
                        else -> "$resultCount notes illuminated"
                    },
                    color = Color(0xFF7A9FFF).copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Text(
                    text = "Point your telescope at any note",
                    color = Color(0xFF4A5A77),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // --- Result chips (tappable, navigate to matched body) ---
            if (matchedBodies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(matchedBodies, key = { it.first }) { (bodyId, bodyName) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    color = Color(0xFF1A2840),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onNavigateToResult(bodyId)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            // Glow dot indicator
                            Text(
                                text = "\u2022",
                                color = Color(0xFF7A9FFF),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = bodyName,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Auto-focus the text field when overlay appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * State holder for the telescope search feature.
 * Owned by SearchViewModel; exposed as Compose state.
 */
data class TelescopeState(
    val isActive: Boolean = false,
    val query: String = "",
    val matchedBodyIds: Set<String> = emptySet(),
    val resultCount: Int = 0
)
