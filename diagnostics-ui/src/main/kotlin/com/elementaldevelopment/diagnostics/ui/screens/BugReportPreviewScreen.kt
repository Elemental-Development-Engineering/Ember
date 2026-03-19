package com.elementaldevelopment.diagnostics.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.ui.state.BugReportStateHolder

/**
 * Full-screen bug report preview with note input, toggles, and export actions.
 *
 * Accepts the [Diagnostics] container as its dependency. Preview text is
 * generated from the real builder/exporter — no formatting logic is
 * duplicated in the UI layer.
 */
@Composable
fun BugReportPreviewScreen(
    diagnostics: Diagnostics,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateHolder = remember { BugReportStateHolder(diagnostics) }
    val state = stateHolder.state

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Bug Report Preview",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Diagnostics remain on your device until you choose to share them. " +
                "After sharing, temporary diagnostics are cleared for your privacy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.userNote,
            onValueChange = { stateHolder.updateUserNote(it) },
            label = { Text("Add a note (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ToggleRow("Include device info", state.includeDeviceInfo) {
            stateHolder.toggleDeviceInfo(it)
        }
        ToggleRow("Include OS info", state.includeOsInfo) {
            stateHolder.toggleOsInfo(it)
        }
        ToggleRow("Include recent logs", state.includeRecentLogs) {
            stateHolder.toggleRecentLogs(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = state.previewText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                modifier = Modifier
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState()),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = { onCopy(stateHolder.getExportText()) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Copy")
            }

            Button(
                onClick = { onShare(stateHolder.getExportText()) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Share")
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
