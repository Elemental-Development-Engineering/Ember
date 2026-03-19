package com.elementaldevelopment.diagnostics.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.api.create
import com.elementaldevelopment.diagnostics.logging.runCatchingLogged
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.ui.actions.DiagnosticsExportHandler
import com.elementaldevelopment.diagnostics.ui.components.DiagnosticsSettingsCard
import com.elementaldevelopment.diagnostics.ui.screens.BugReportPreviewScreen

class MainActivity : ComponentActivity() {

    private lateinit var diagnostics: Diagnostics
    private lateinit var exportHandler: DiagnosticsExportHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize diagnostics once during app startup
        diagnostics = Diagnostics.create(
            context = applicationContext,
            config = GeneralUtilityAppConfig(),
        )

        exportHandler = DiagnosticsExportHandler(this, diagnostics)

        // Example logging calls that an app might make
        diagnostics.logger.log("FileIO", "Document list loaded")
        diagnostics.logger.log(DiagnosticLevel.WARN, "Parser", "Unsupported syntax encountered")

        diagnostics.logger.runCatchingLogged("Timer", "schedule notification") {
            // Simulated operation that might fail
        }

        // Simulate an error
        diagnostics.logger.logError(
            "Parser",
            "Failed to render preview",
            RuntimeException("OutOfMemoryError while rendering"),
        )

        var showPreview by mutableStateOf(false)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showPreview) {
                        BugReportPreviewScreen(
                            diagnostics = diagnostics,
                            onCopy = { text ->
                                if (exportHandler.copyToClipboard(text)) {
                                    Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show()
                                }
                                showPreview = false
                            },
                            onShare = { text ->
                                exportHandler.shareReport(text)
                                showPreview = false
                            },
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "Ember Sample App",
                                style = MaterialTheme.typography.headlineMedium,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "This app demonstrates how to integrate " +
                                    "the Ember diagnostics library.",
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = {
                                diagnostics.logger.log("UserAction", "Button tapped")
                            }) {
                                Text("Log a sample event")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(onClick = {
                                diagnostics.logger.logError(
                                    "Network",
                                    "Request failed",
                                    java.io.IOException("Connection timeout"),
                                )
                            }) {
                                Text("Log a sample error")
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            DiagnosticsSettingsCard(
                                onSendBugReport = { showPreview = true },
                            )
                        }
                    }
                }
            }
        }
    }
}
