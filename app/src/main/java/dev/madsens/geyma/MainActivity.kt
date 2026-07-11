package dev.madsens.geyma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            GeymaTheme(theme = theme) {
                GeymaRoot(app)
            }
        }
    }
}
