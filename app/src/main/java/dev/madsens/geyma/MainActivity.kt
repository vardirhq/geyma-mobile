package dev.madsens.geyma

import android.graphics.Color as AndroidColor
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import dev.madsens.geyma.theme.GeymaTheme
import dev.madsens.geyma.ui.GeymaRoot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    // Files shared into Geyma from other apps, surfaced to the UI to file into a set.
    private var sharedUris by mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GeymaApp
        sharedUris = extractSharedUris(intent)

        // Paint the window in the saved skin's background before Compose lays out,
        // so a light skin doesn't flash the black manifest window background on
        // launch. The DataStore read is a fast first-value fetch.
        val startTheme = runBlocking { app.prefs.theme.first() }
        window.setBackgroundDrawable(ColorDrawable(startTheme.bg.toArgb()))

        setContent {
            val theme by app.prefs.theme.collectAsState(initial = startTheme)

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
                GeymaRoot(
                    app = app,
                    sharedUris = sharedUris,
                    onSharedConsumed = { sharedUris = emptyList() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incoming = extractSharedUris(intent)
        if (incoming.isNotEmpty()) sharedUris = incoming
    }

    private fun extractSharedUris(intent: Intent?): List<Uri> {
        intent ?: return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty().filterNotNull()
            }
            else -> emptyList()
        }
    }
}
