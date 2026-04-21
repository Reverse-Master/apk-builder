package com.apkbuilder.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkbuilder.app.model.ProjectConfig
import com.apkbuilder.app.model.TabSpec
import java.io.File

/**
 * Drag-free, but fully form-driven customisation panel. Every field mutates
 * the shared [ProjectConfig] in place; the preview screen re-reads it.
 */
@Composable
fun EditorScreen(cfg: ProjectConfig, onPreview: () -> Unit, onBuild: () -> Unit) {
    val ctx = LocalContext.current
    var bump by remember { mutableStateOf(0) } // forces recomposition on mutation
    fun touch() { bump++ }

    val pickIcon = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { cfg.iconPath = copyToCache(ctx, it, "icon.png"); touch() }
    }
    val pickSplash = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { cfg.splashImagePath = copyToCache(ctx, it, "splash.png"); touch() }
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { cfg.audioPath = copyToCache(ctx, it, "audio.mp3"); touch() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Customize", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // --- Identity --------------------------------------------------------
        Section("App identity") {
            OutlinedTextField(cfg.appName, { cfg.appName = it; touch() },
                label = { Text("App name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cfg.packageName, { cfg.packageName = it; touch() },
                label = { Text("Package name (e.g. com.example.app)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cfg.versionName, { cfg.versionName = it; touch() },
                label = { Text("Version name") }, modifier = Modifier.fillMaxWidth())
        }

        // --- Header / bars ---------------------------------------------------
        Section("Header & bars") {
            OutlinedTextField(cfg.headerTitle, { cfg.headerTitle = it; touch() },
                label = { Text("Header title") }, modifier = Modifier.fillMaxWidth())
            ToggleRow("Show top bar", cfg.showTopBar) { cfg.showTopBar = it; touch() }
            ToggleRow("Show bottom bar", cfg.showBottomBar) { cfg.showBottomBar = it; touch() }
        }

        // --- Colours ---------------------------------------------------------
        Section("Colours") {
            ColorSliders("Primary", cfg.primaryColor) { cfg.primaryColor = it; touch() }
            ColorSliders("Accent", cfg.accentColor) { cfg.accentColor = it; touch() }
            ColorSliders("Background", cfg.backgroundColor) { cfg.backgroundColor = it; touch() }
        }

        // --- Window size -----------------------------------------------------
        Section("Window size (0 = fill screen)") {
            NumberRow("Width (dp)", cfg.windowWidthDp) { cfg.windowWidthDp = it; touch() }
            NumberRow("Height (dp)", cfg.windowHeightDp) { cfg.windowHeightDp = it; touch() }
        }

        // --- Media -----------------------------------------------------------
        Section("Media") {
            MediaRow("App icon", cfg.iconPath) { pickIcon.launch("image/*") }
            MediaRow("Splash image", cfg.splashImagePath) { pickSplash.launch("image/*") }
            MediaRow("Audio clip", cfg.audioPath) { pickAudio.launch("audio/*") }
        }

        // --- Tabs ------------------------------------------------------------
        Section("Tabs") {
            cfg.tabs.forEachIndexed { i, tab ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(tab.title, { cfg.tabs[i] = tab.copy(title = it); touch() },
                        label = { Text("Title") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(tab.icon, { cfg.tabs[i] = tab.copy(icon = it); touch() },
                        label = { Text("Icon") }, modifier = Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ cfg.tabs.add(TabSpec("Tab", "star")); touch() }) { Text("Add tab") }
                OutlinedButton({ if (cfg.tabs.size > 1) { cfg.tabs.removeAt(cfg.tabs.lastIndex); touch() } }) { Text("Remove last") }
            }
        }

        // --- Actions ---------------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onPreview, modifier = Modifier.weight(1f)) { Text("Preview") }
            Button(onBuild, modifier = Modifier.weight(1f)) { Text("Build APK") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label); Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun NumberRow(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: 0) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ColorSliders(label: String, argb: Int, onChange: (Int) -> Unit) {
    val a = (argb ushr 24) and 0xFF
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(String.format("#%08X", argb), style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .background(Color(argb), shape = MaterialTheme.shapes.small)
                    .size(28.dp)
            )
            Column(Modifier.weight(1f)) {
                ChannelSlider("R", r) { onChange((a shl 24) or (it shl 16) or (g shl 8) or b) }
                ChannelSlider("G", g) { onChange((a shl 24) or (r shl 16) or (it shl 8) or b) }
                ChannelSlider("B", b) { onChange((a shl 24) or (r shl 16) or (g shl 8) or it) }
            }
        }
    }
}

@Composable
private fun ChannelSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.padding(end = 8.dp))
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..255f)
    }
}

@Composable
private fun MediaRow(label: String, currentPath: String?, onPick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(currentPath?.substringAfterLast('/') ?: "(none selected)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onPick) { Text("Choose") }
    }
}

private fun copyToCache(ctx: android.content.Context, uri: Uri, name: String): String {
    val out = File(ctx.cacheDir, "import_${System.currentTimeMillis()}_$name")
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        out.outputStream().use { input.copyTo(it) }
    }
    return out.absolutePath
}


