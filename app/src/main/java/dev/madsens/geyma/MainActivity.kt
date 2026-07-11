package dev.madsens.geyma

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.madsens.geyma.theme.GeymaTheme
import dev.madsens.geyma.theme.resolveTheme
import dev.madsens.geyma.ui.GeymaRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GeymaApp
        setContent {
            val theme by app.prefs.theme.collectAsState(initial = resolveTheme("obsidian"))

            // Status/nav bar icon contrast must follow the skin, not the OS
            // dark-mode setting — a light skin on a dark-mode phone would
            // otherwise get white icons on a light background.
            LaunchedEffect(theme.isDark) {
                val style = if (theme.isDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Let the themed bottom bar show through behind the
                    // 3-button navigation instead of a forced scrim.
                    window.isNavigationBarContrastEnforced = false
                }
            }

            GeymaTheme(theme = theme) {
                GeymaRoot(app)
            }
        }
    }
}
