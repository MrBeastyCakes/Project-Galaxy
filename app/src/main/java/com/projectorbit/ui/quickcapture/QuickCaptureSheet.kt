package com.projectorbit.ui.quickcapture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.projectorbit.ui.theme.OrbitSurface

/**
 * Bottom sheet for quick stardust (asteroid) capture.
 *
 * User types a thought and taps Send — an asteroid is created in the
 * outer belt with the text as content.
 *
 * @param onCapture called with the text content when user confirms
 * @param onDismiss called when the sheet is dismissed without capturing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    onCapture: (text: String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    val submit = {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            onCapture(trimmed)
            text = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .imePadding()
        ) {
            Text(
                text = "Quick Capture",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "This thought will become an asteroid in the outer belt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Capture a thought…") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() })
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { submit() },
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Send to Belt")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
