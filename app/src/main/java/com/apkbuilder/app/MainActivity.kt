package com.apkbuilder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.apkbuilder.app.theme.AppTheme
import com.apkbuilder.app.ui.AppRoot

/**
 * Single entry-point Activity. All screens are composed inside [AppRoot]
 * using Jetpack Compose + Navigation. Keeping a single Activity makes the
 * app smaller and simpler to maintain.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme { AppRoot() }
        }
    }
}
