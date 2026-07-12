package dev.madsens.geyma.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.files.ArchiveSupport
import dev.madsens.geyma.files.InAppViewer
import dev.madsens.geyma.files.StorageRoots
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.geymaBackdrop
import dev.madsens.geyma.theme.onAccent
import dev.madsens.geyma.ui.almanac.AlmanacScreen
import dev.madsens.geyma.ui.archive.ArchiveScreen
import dev.madsens.geyma.ui.browser.AddToSetDialog
import dev.madsens.geyma.ui.browser.BrowserScreen
import dev.madsens.geyma.ui.browser.BrowserViewModel
import dev.madsens.geyma.ui.dossier.DossierScreen
import dev.madsens.geyma.ui.echoes.EchoesScreen
import dev.madsens.geyma.ui.finder.FinderScreen
import dev.madsens.geyma.ui.home.HomeScreen
import dev.madsens.geyma.ui.sets.SetsScreen
import dev.madsens.geyma.ui.browser.openFile
import dev.madsens.geyma.ui.settings.SettingsScreen
import dev.madsens.geyma.ui.sweep.SweepScreen
import dev.madsens.geyma.ui.timeline.TimelineScreen
import dev.madsens.geyma.ui.trash.TrashScreen
import dev.madsens.geyma.ui.viewer.ViewerScreen
import kotlinx.coroutines.launch
import java.io.File

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    FILES("Files", Icons.Filled.Folder),
    TIMELINE("Timeline", Icons.Filled.History),
    SETS("Sets", Icons.AutoMirrored.Filled.PlaylistPlay),
}

@Composable
fun GeymaRoot(
    app: GeymaApp,
    sharedUris: List<Uri> = emptyList(),
    onSharedConsumed: () -> Unit = {},
) {
    val t = LocalTheme.current
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(StorageRoots.hasFullAccess(context)) }

    // Re-check on resume: the all-files-access grant happens in system settings.
    // Also re-scan watched folders so arrivals show up when returning to Geyma.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hasAccess = StorageRoots.hasFullAccess(context)
                    if (hasAccess) {
                        app.watcher.sync()
                        app.watcher.start()
                    }
                }
                Lifecycle.Event.ON_STOP -> app.watcher.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.geymaBackdrop(t)) {
        if (!hasAccess) {
            StorageAccessGate(onGranted = { hasAccess = true })
            return@Box
        }

        val vm: BrowserViewModel = viewModel(factory = BrowserViewModel.factory(app))
        var tab by remember { mutableStateOf(Tab.HOME) }
        var trashOpen by remember { mutableStateOf(false) }
        var settingsOpen by remember { mutableStateOf(false) }
        var finderOpen by remember { mutableStateOf(false) }
        var sweepOpen by remember { mutableStateOf(false) }
        var almanacOpen by remember { mutableStateOf(false) }
        var echoesOpen by remember { mutableStateOf(false) }
        var dossierPath by remember { mutableStateOf<String?>(null) }
        var viewerPath by remember { mutableStateOf<String?>(null) }
        var archivePath by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        val overlayOpen = trashOpen || settingsOpen || finderOpen || sweepOpen ||
            almanacOpen || echoesOpen || dossierPath != null || viewerPath != null ||
            archivePath != null
        fun closeOverlays() {
            trashOpen = false
            settingsOpen = false
            finderOpen = false
            sweepOpen = false
            almanacOpen = false
            echoesOpen = false
            dossierPath = null
            viewerPath = null
            archivePath = null
        }

        // Tapping a file: browse into it if it's a zip we can read, otherwise open
        // it inside Geyma when we have a viewer, otherwise hand off to the system
        // chooser. Every path counts as an "open".
        fun openEntry(path: String) {
            val file = File(path)
            when {
                ArchiveSupport.canBrowse(file.name) -> {
                    closeOverlays()
                    archivePath = path
                }
                InAppViewer.canView(file.name, file.length()) -> {
                    closeOverlays()
                    viewerPath = path
                }
                else -> openFile(context, path) { scope.launch { app.repo.recordOpen(it) } }
            }
        }

        // Files shared into Geyma: copy them into the inbox, then offer a set to file them in.
        var intakePaths by remember { mutableStateOf<List<String>?>(null) }
        LaunchedEffect(sharedUris) {
            if (sharedUris.isNotEmpty()) {
                val paths = dev.madsens.geyma.ui.share.ShareIntake.intake(context, app, sharedUris, fromApp = null)
                if (paths.isNotEmpty()) intakePaths = paths
                onSharedConsumed()
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = t.surface) {
                    for (candidate in Tab.entries) {
                        NavigationBarItem(
                            selected = tab == candidate && !overlayOpen,
                            onClick = {
                                closeOverlays()
                                tab = candidate
                            },
                            icon = { Icon(candidate.icon, candidate.label) },
                            label = { Text(candidate.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = t.accent,
                                selectedTextColor = t.accent,
                                indicatorColor = t.accent.copy(alpha = 0.15f),
                                unselectedIconColor = t.inkFaint,
                                unselectedTextColor = t.inkFaint,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    viewerPath != null -> {
                        BackHandler { viewerPath = null }
                        ViewerScreen(
                            app = app,
                            initialPath = viewerPath!!,
                            onBack = { viewerPath = null },
                        )
                    }
                    archivePath != null -> {
                        // ArchiveScreen owns its own back handling (internal folders + preview).
                        ArchiveScreen(
                            app = app,
                            archivePath = archivePath!!,
                            onBack = { archivePath = null },
                            onExtracted = { folder ->
                                archivePath = null
                                vm.open(folder)
                                tab = Tab.FILES
                            },
                        )
                    }
                    finderOpen -> {
                        BackHandler { finderOpen = false }
                        FinderScreen(
                            app = app,
                            onBack = { finderOpen = false },
                            onView = { openEntry(it) },
                            onBrowseTo = { path ->
                                finderOpen = false
                                vm.open(path)
                                tab = Tab.FILES
                            },
                        )
                    }
                    sweepOpen -> {
                        BackHandler { sweepOpen = false }
                        SweepScreen(
                            app = app,
                            onBack = { sweepOpen = false },
                            onOpenTrash = {
                                sweepOpen = false
                                trashOpen = true
                            },
                        )
                    }
                    dossierPath != null -> {
                        BackHandler { dossierPath = null }
                        DossierScreen(
                            app = app,
                            path = dossierPath!!,
                            onBack = { dossierPath = null },
                            onBrowse = { path ->
                                closeOverlays()
                                vm.open(path)
                                tab = Tab.FILES
                            },
                        )
                    }
                    almanacOpen -> {
                        BackHandler { almanacOpen = false }
                        AlmanacScreen(
                            app = app,
                            onBack = { almanacOpen = false },
                            onBrowse = { path ->
                                almanacOpen = false
                                vm.open(path)
                                tab = Tab.FILES
                            },
                            onOpenDossier = { path -> dossierPath = path },
                        )
                    }
                    echoesOpen -> {
                        BackHandler { echoesOpen = false }
                        EchoesScreen(
                            app = app,
                            onBack = { echoesOpen = false },
                            onOpenTrash = {
                                echoesOpen = false
                                trashOpen = true
                            },
                            onOpenDossier = { path -> dossierPath = path },
                        )
                    }
                    trashOpen -> {
                        BackHandler { trashOpen = false }
                        TrashScreen(app)
                    }
                    settingsOpen -> {
                        BackHandler { settingsOpen = false }
                        SettingsScreen(
                            app = app,
                            onBack = { settingsOpen = false },
                            onOpenTrash = { trashOpen = true },
                        )
                    }
                    else -> when (tab) {
                        Tab.HOME -> HomeScreen(
                            app = app,
                            onBrowse = { path ->
                                vm.open(path)
                                tab = Tab.FILES
                            },
                            onOpenTimeline = { tab = Tab.TIMELINE },
                            onOpenTrash = { trashOpen = true },
                            onOpenSettings = { settingsOpen = true },
                            onOpenFinder = { finderOpen = true },
                            onOpenSweep = { sweepOpen = true },
                            onOpenAlmanac = { almanacOpen = true },
                            onOpenEchoes = { echoesOpen = true },
                            onOpenDossier = { path -> dossierPath = path },
                        )
                        Tab.FILES -> BrowserScreen(app, vm, onView = { openEntry(it.path) })
                        Tab.TIMELINE -> TimelineScreen(app) { path ->
                            vm.open(path)
                            tab = Tab.FILES
                        }
                        Tab.SETS -> SetsScreen(
                            app = app,
                            onView = { openEntry(it) },
                            onBrowseTo = { path ->
                                vm.open(path)
                                tab = Tab.FILES
                            },
                        )
                    }
                }
            }
        }

        intakePaths?.let { paths ->
            AddToSetDialog(
                app = app,
                paths = paths,
                onDone = { intakePaths = null },
            )
        }
    }
}

@Composable
private fun StorageAccessGate(onGranted: () -> Unit) {
    val t = LocalTheme.current
    val context = LocalContext.current
    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) onGranted()
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        Icon(Icons.Filled.Lock, null, tint = t.accent, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(24.dp))
        Text("Geyma needs the keys", color = t.ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "To browse and guard your files, Geyma needs access to your storage. " +
                "Nothing leaves your device — the journal lives locally, like everything else.",
            color = t.inkSoft,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    runCatching { context.startActivity(intent) }.onFailure {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                } else {
                    legacyLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        ),
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = t.accent, contentColor = t.onAccent),
        ) {
            Text("Grant storage access")
        }
    }
}
