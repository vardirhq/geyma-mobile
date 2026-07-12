package dev.madsens.geyma.ui.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.files.PathUtils
import java.io.File

/**
 * Zoomable, pannable image viewer that swipes through every image sibling in the
 * same folder — so a tap on one photo becomes a gallery. Formats the device
 * can't decode fall back to a broken-image marker (the top bar still offers an
 * external app).
 */
@Composable
fun ImageViewer(app: GeymaApp, path: String, onPathChanged: (String) -> Unit) {
    val dir = PathUtils.parentOf(path)
    var images by remember(dir) { mutableStateOf(listOf(path)) }

    LaunchedEffect(dir) {
        if (dir == null) return@LaunchedEffect
        val siblings = runCatching {
            app.repo.listDir(dir, showHidden = true)
                .filter { !it.isDir && it.kind == FileKind.IMAGE }
                .map { it.path }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { PathUtils.nameOf(it) })
        }.getOrNull().orEmpty()
        if (siblings.isNotEmpty()) images = siblings
    }

    // Recreate the pager once the sibling list resolves so it starts on the tapped image.
    key(images) {
        val start = images.indexOf(path).coerceAtLeast(0)
        val pagerState = rememberPagerState(initialPage = start, pageCount = { images.size })

        LaunchedEffect(pagerState, images) {
            snapshotFlow { pagerState.currentPage }.collect { p ->
                images.getOrNull(p)?.let(onPathChanged)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableImage(images[page])
        }
    }
}

@Composable
private fun ZoomableImage(path: String) {
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offsetX by remember(path) { mutableFloatStateOf(0f) }
    var offsetY by remember(path) { mutableFloatStateOf(0f) }
    var failed by remember(path) { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (failed) {
            ViewerMessage("Couldn't decode this image.", icon = Icons.Filled.BrokenImage)
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(File(path)).build(),
                contentDescription = PathUtils.nameOf(path),
                contentScale = ContentScale.Fit,
                onState = { state -> if (state is AsyncImagePainter.State.Error) failed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
                    .transformable(transformState)
                    .pointerInput(path) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            },
                        )
                    },
            )
        }
    }
}
