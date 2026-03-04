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
import androidx.compose.material3.Icon
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

// Heading span sizes — used for both applying and detecting active state
private val H1_SIZE = 28.sp
private val H2_SIZE = 22.sp
private val H3_SIZE = 18.sp

/**
 * Rich text editor using the Compose-RichEditor library.
 * Supports: bold, italic, headings (H1-H3 via SpanStyle), bullet lists, numbered lists.
 *
 * The library (v1.0.0-rc05) has no heading ParagraphType, so headings use SpanStyle
 * (fontSize + bold). Active-state detection checks currentSpanStyle.fontSize.
 *
 * [onContentChanged] is called with (richTextHtml, plainText) whenever content changes.
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

    // Observe text changes via snapshotFlow — auto-save debounce is handled in ViewModel
    LaunchedEffect(richTextState) {
        snapshotFlow { richTextState.annotatedString }
            .collect {
                onContentChanged(
                    richTextState.toHtml(),
                    richTextState.annotatedString.text
                )
            }
    }

    // Derive active states from currentSpanStyle
    val currentSpanStyle = richTextState.currentSpanStyle
    val isBold = currentSpanStyle.fontWeight?.weight == 700
    val isItalic = currentSpanStyle.fontStyle == FontStyle.Italic
    val currentFontSize = currentSpanStyle.fontSize
    val isH1 = currentFontSize == H1_SIZE
    val isH2 = currentFontSize == H2_SIZE
    val isH3 = currentFontSize == H3_SIZE
    val isUnorderedList = richTextState.isUnorderedList
    val isOrderedList = richTextState.isOrderedList

    Column(modifier = modifier) {
        FormattingToolbar(
            isBold = isBold,
            isItalic = isItalic,
            isH1 = isH1,
            isH2 = isH2,
            isH3 = isH3,
            isUnorderedList = isUnorderedList,
            isOrderedList = isOrderedList,
            onBold = {
                richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            },
            onItalic = {
                richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            },
            onH1 = {
                // Clear other heading sizes before applying H1
                if (!isH1) {
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H2_SIZE))
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H3_SIZE))
                    richTextState.addSpanStyle(SpanStyle(fontSize = H1_SIZE, fontWeight = FontWeight.Bold))
                } else {
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H1_SIZE))
                }
            },
            onH2 = {
                if (!isH2) {
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H1_SIZE))
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H3_SIZE))
                    richTextState.addSpanStyle(SpanStyle(fontSize = H2_SIZE, fontWeight = FontWeight.Bold))
                } else {
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H2_SIZE))
                }
            },
            onH3 = {
                if (!isH3) {
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H1_SIZE))
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H2_SIZE))
                    richTextState.addSpanStyle(SpanStyle(fontSize = H3_SIZE, fontWeight = FontWeight.Bold))
                } else {
                    richTextState.removeSpanStyle(SpanStyle(fontSize = H3_SIZE))
                }
            },
            onBulletList = { richTextState.toggleUnorderedList() },
            onNumberedList = { richTextState.toggleOrderedList() }
        )

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
    isH1: Boolean,
    isH2: Boolean,
    isH3: Boolean,
    isUnorderedList: Boolean,
    isOrderedList: Boolean,
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
            IconToggleButton(checked = isH1, onCheckedChange = { onH1() }) {
                Text(
                    "H1",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isH1) OrbitAccent else MaterialTheme.colorScheme.onSurface
                )
            }
            IconToggleButton(checked = isH2, onCheckedChange = { onH2() }) {
                Text(
                    "H2",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isH2) OrbitAccent else MaterialTheme.colorScheme.onSurface
                )
            }
            IconToggleButton(checked = isH3, onCheckedChange = { onH3() }) {
                Text(
                    "H3",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isH3) OrbitAccent else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(4.dp))
            IconToggleButton(checked = isUnorderedList, onCheckedChange = { onBulletList() }) {
                Icon(
                    Icons.Default.FormatListBulleted,
                    contentDescription = "Bullet list",
                    tint = if (isUnorderedList) OrbitAccent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconToggleButton(checked = isOrderedList, onCheckedChange = { onNumberedList() }) {
                Icon(
                    Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered list",
                    tint = if (isOrderedList) OrbitAccent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
