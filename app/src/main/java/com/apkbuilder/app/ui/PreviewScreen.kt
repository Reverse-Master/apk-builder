package com.apkbuilder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkbuilder.app.model.ProjectConfig

/**
 * Renders an in-app phone-frame preview of the user's app using the current
 * [ProjectConfig]. This is a pure-Compose mock - it shows the chosen colours,
 * header, tabs and window size without actually running the template's code.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(cfg: ProjectConfig, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Preview") },
            navigationIcon = {
                IconButton(onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            // Phone frame.
            Column(
                Modifier
                    .fillMaxWidth(0.85f)
                    .height(560.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF111114))
                    .padding(8.dp)
            ) {
                MockApp(cfg, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)))
            }
        }
    }
}

@Composable
private fun MockApp(cfg: ProjectConfig, modifier: Modifier) {
    Column(modifier.background(Color(cfg.backgroundColor))) {
        if (cfg.showTopBar) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(cfg.primaryColor)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(cfg.headerTitle, color = Color.White, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp))
            }
        }

        Column(
            Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(cfg.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = Color(cfg.primaryColor))
            Text("Template: ${cfg.templateId}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            repeat(3) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(cfg.accentColor).copy(alpha = 0.18f)))
            }
        }

        if (cfg.showBottomBar) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).background(Color(cfg.primaryColor).copy(alpha = 0.92f)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                cfg.tabs.forEach { tab ->
                    Text(tab.title, color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
