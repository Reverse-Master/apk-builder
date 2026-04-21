package com.apkbuilder.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkbuilder.app.builder.ApkAssembler
import com.apkbuilder.app.builder.ApkInstaller
import com.apkbuilder.app.model.AbiTarget
import com.apkbuilder.app.model.ProjectConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Final step of the flow. The user picks the ABI target (32, 64, or both),
 * taps Build, and we run [ApkAssembler] off the main thread. When done we
 * surface an "Install" button that hands the file to the system installer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(cfg: ProjectConfig, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var abi by remember { mutableStateOf(AbiTarget.UNIVERSAL) }
    var building by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to build.") }
    var output by remember { mutableStateOf<File?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Build APK") },
            navigationIcon = {
                IconButton(onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
        )

        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target architecture", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Choose 32-bit for older devices, 64-bit for the smallest size on modern phones, or Universal for maximum compatibility.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Kotlin 1.9+: prefer enum.entries over the deprecated values().
                    AbiTarget.entries.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = abi == option, onClick = { abi = option })
                            Text(option.label, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (building) CircularProgressIndicator(Modifier.padding(end = 12.dp))
                        Text(status)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        building = true
                        status = "Starting build..."
                        output = null
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    ApkAssembler(ctx).build(cfg, abi) { msg -> status = msg }
                                }
                                output = file
                                status = "Built: ${file.name} (${file.length() / 1024} KB)"
                            } catch (e: Throwable) {
                                status = "Build failed: ${e.message}"
                            } finally {
                                building = false
                            }
                        }
                    },
                    enabled = !building,
                    modifier = Modifier.weight(1f)
                ) { Text(if (building) "Building..." else "Build APK") }

                OutlinedButton(
                    onClick = { output?.let { ApkInstaller.install(ctx, it) } },
                    enabled = output != null && !building,
                    modifier = Modifier.weight(1f)
                ) { Text("Install") }
            }
        }
    }
}
