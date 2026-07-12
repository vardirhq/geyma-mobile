package dev.madsens.geyma.ui.viewer

import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.madsens.geyma.theme.LocalTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/** Plays local video with the standard scrubbing controls, using the platform decoders. */
@Composable
fun VideoViewer(path: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    setVideoPath(path)
                    setOnPreparedListener { it.start() }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Minimal audio player: play/pause and a scrubber, driven by [MediaPlayer]. */
@Composable
fun AudioViewer(path: String, name: String) {
    val t = LocalTheme.current
    val player = remember(path) { MediaPlayer() }
    var prepared by remember(path) { mutableStateOf(false) }
    var playing by remember(path) { mutableStateOf(false) }
    var duration by remember(path) { mutableIntStateOf(0) }
    var position by remember(path) { mutableIntStateOf(0) }
    var failed by remember(path) { mutableStateOf(false) }

    DisposableEffect(path) {
        runCatching {
            player.setDataSource(path)
            player.setOnPreparedListener {
                prepared = true
                duration = it.duration.coerceAtLeast(0)
                it.start()
                playing = true
            }
            player.setOnCompletionListener {
                playing = false
                position = duration
            }
            player.prepareAsync()
        }.onFailure { failed = true }
        onDispose { runCatching { player.release() } }
    }

    LaunchedEffect(playing, prepared) {
        while (playing && prepared) {
            position = runCatching { player.currentPosition }.getOrDefault(position)
            delay(500)
        }
    }

    if (failed) {
        ViewerMessage("Couldn't play this audio file.", icon = Icons.Filled.MusicNote)
        return
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                null,
                tint = t.accent,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            name,
            color = t.ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Slider(
            value = position.toFloat(),
            onValueChange = {
                position = it.toInt()
                runCatching { player.seekTo(position) }
            },
            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
            enabled = prepared,
            colors = SliderDefaults.colors(
                thumbColor = t.accent,
                activeTrackColor = t.accent,
                inactiveTrackColor = t.border,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(position), color = t.inkFaint, fontSize = 12.sp)
            Text(formatMs(duration), color = t.inkFaint, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        IconButton(
            onClick = {
                if (!prepared) return@IconButton
                if (playing) {
                    runCatching { player.pause() }
                    playing = false
                } else {
                    runCatching { player.start() }
                    playing = true
                }
            },
            modifier = Modifier.size(64.dp),
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playing) "Pause" else "Play",
                tint = t.accent,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.toLong())
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
