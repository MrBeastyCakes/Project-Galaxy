package com.projectorbit.ui.surface

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.projectorbit.ui.theme.OrbitAccent
import com.projectorbit.ui.theme.OrbitSurfaceVariant

/**
 * Rich text editor using the Compose-RichEditor library.
 * Supports: bold, italic, headings (H1-H3), bullet lists, numbered lists.
 *
 * [onContentChanged] is called with (richTextJson, plainText) whenever content changes.
 */
@Composable
fun OrbitRichTextEditor(
    initialJson: String = "",
    onContentChanged: (richTextJson: String, plainText: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val richTextState = rememberRichTextState()

    // Load initial content
    remember(initialJson) {
        if (initialJson.isNotBlank()) {
            try {
                richTextState.setHtml(initialJson)
            } catch (e: Exception) {
                // Fallback: treat as plain text
                richTextState.setText(initialJson)
            }
        }
    }

    // Observe text changes via snapshotFlow
    LaunchedEffect(richTextState) {
        snapshotFlow { richTextState.annotatedString }
            .collect {
                onContentChanged(
                    richTextState.toHtml(),
                    richTextState.annotatedString.text
                )
            }
    }

    Column(modifier = modifier) {
        // Formatting toolbar
        FormattingToolbar(
            isBold = richTextState.currentSpanStyle.fontWeight?.weight == 700,
            isItalic = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
            onBold = { richTextState.toggleSpanStyle(
                SpanStyle(fontWeight = FontWeight.Bold)
            ) },
            onItalic = { richTextState.toggleSpanStyle(
                SpanStyle(fontStyle = FontStyle.Italic)
            ) },
            onH1 = { richTextState.toggleSpanStyle(
                SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
            ) },
            onH2 = { richTextState.toggleSpanStyle(
                SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
            ) },
            onH3 = { richTextState.toggleSpanStyle(
                SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            ) },
            onBulletList = { richTextState.toggleUnorderedList() },
            onNumberedList = { richTextState.toggleOrderedList() }
        )

        // Editor
        RichTextEditor(
            state = richTextState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun FormattingToolbar(
    isBold: Boolean,
    isItalic: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onH1: () -> Unit,
    onH2: () -> Unit,
    onH3: () -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OrbitSurfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconToggleButton(checked = isBold, onCheckedChange = { onBold() }) {
                Icon(
                    Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    tint = if (isBold) OrbitAccent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconToggleButton(checked = isItalic, onCheckedChange = { onItalic() }) {
                Icon(
                    Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    tint = if (isItalic) OrbitAccent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onH1) {
                Text("H1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onH2) {
                Text("H2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onH3) {
                Text("H3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onBulletList) {
                Icon(
                    Icons.Default.FormatListBulleted,
                    contentDescription = "Bullet list",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onNumberedList) {
                Icon(
                    Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered list",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
