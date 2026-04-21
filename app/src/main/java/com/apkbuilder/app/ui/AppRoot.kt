package com.apkbuilder.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apkbuilder.app.model.ProjectConfig

/**
 * Top-level scaffold with a bottom navigation bar. The actual workflow
 * (Templates -> Editor -> Preview -> Build) lives inside the Templates tab so
 * the user is guided through a single linear flow.
 */
@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Templates" to Icons.Default.Apps,
                      "Projects"  to Icons.Default.Folder,
                      "Settings"  to Icons.Default.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> TemplatesFlow(Modifier.fillMaxSize().padding(padding))
            1 -> ProjectsScreen(Modifier.fillMaxSize().padding(padding))
            else -> SettingsScreen(Modifier.fillMaxSize().padding(padding))
        }
    }
}

/**
 * Linear sub-flow inside the Templates tab. Compose's NavHost gives us
 * back-stack and arguments without dragging in a heavy dependency.
 */
@Composable
fun TemplatesFlow(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    // Shared mutable config edited across the editor / preview / build steps.
    val workingConfig = remember { mutableStateOf<ProjectConfig?>(null) }

    NavHost(navController = nav, startDestination = "gallery", modifier = modifier) {
        composable("gallery") {
            TemplatesScreen(onPick = { template ->
                workingConfig.value = ProjectConfig(templateId = template.id, appName = template.name)
                nav.navigate("editor")
            })
        }
        composable("editor")  { workingConfig.value?.let { EditorScreen(it, onPreview = { nav.navigate("preview") }, onBuild = { nav.navigate("build") }) } }
        composable("preview") { workingConfig.value?.let { PreviewScreen(it, onBack = { nav.popBackStack() }) } }
        composable("build")   { workingConfig.value?.let { BuildScreen(it, onBack = { nav.popBackStack() }) } }
    }
}
