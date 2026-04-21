package com.apkbuilder.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkbuilder.app.builder.ProjectStore

@Composable
fun ProjectsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val store = remember { ProjectStore(ctx) }
    var items by remember { mutableStateOf(store.list()) }

    Column(modifier.padding(20.dp)) {
        Text("My Projects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Saved projects appear here. Use the Templates tab to create a new one.",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))

        if (items.isEmpty()) {
            Text("No projects yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(items) { (name, cfg) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            Text("Template: ${cfg.templateId} * ${cfg.appName}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
