package dev.madsens.geyma.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.data.Prefs
import dev.madsens.geyma.data.SortDir
import dev.madsens.geyma.data.SortKey
import dev.madsens.geyma.data.ViewPrefs
import dev.madsens.geyma.files.Entry
import dev.madsens.geyma.files.FsRepository
import dev.madsens.geyma.files.GhostTrail
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.StorageRoots
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class ClipMode { COPY, MOVE }

data class Clipboard(val paths: List<String>, val mode: ClipMode)

data class BrowserState(
    val dir: String = StorageRoots.primaryPath(),
    val rootPath: String = StorageRoots.primaryPath(),
    val rootLabel: String = "Internal storage",
    val entries: List<Entry> = emptyList(),
    val ghosts: List<GhostTrail> = emptyList(),
    val loading: Boolean = true,
    val query: String = "",
    val selection: Set<String> = emptySet(),
    val clipboard: Clipboard? = null,
    val error: String? = null,
    val prefs: ViewPrefs = ViewPrefs(),
) {
    val selecting: Boolean get() = selection.isNotEmpty()

    val visibleEntries: List<Entry>
        get() {
            val filtered = if (query.isBlank()) entries else entries.filter { it.name.contains(query, ignoreCase = true) }
            val cmp: Comparator<Entry> = when (prefs.sortKey) {
                SortKey.NAME -> compareBy { it.name.lowercase() }
                SortKey.KIND -> compareBy<Entry> { it.kind }.thenBy { it.name.lowercase() }
                SortKey.SIZE -> compareBy<Entry> { it.size }.thenBy { it.name.lowercase() }
                SortKey.MODIFIED -> compareBy<Entry> { it.modifiedMs }.thenBy { it.name.lowercase() }
            }
            val sorted = filtered.sortedWith(if (prefs.sortDir == SortDir.DESC) cmp.reversed() else cmp)
            return sorted.sortedByDescending { it.isDir }
        }

    val breadcrumbs: List<Pair<String, String>>
        get() = PathUtils.breadcrumbs(dir, rootPath, rootLabel)
}

class BrowserViewModel(private val repo: FsRepository, private val prefs: Prefs) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.viewPrefs.collect { vp ->
                val hiddenChanged = vp.showHidden != _state.value.prefs.showHidden
                _state.value = _state.value.copy(prefs = vp)
                if (hiddenChanged) refresh()
            }
        }
        refresh()
    }

    fun open(dir: String) {
        _state.value = _state.value.copy(dir = dir, query = "", selection = emptySet())
        refresh()
    }

    /** Back navigation: up one level until the root, then signal false. */
    fun up(): Boolean {
        val s = _state.value
        if (s.dir == s.rootPath) return false
        val parent = PathUtils.parentOf(s.dir) ?: return false
        open(parent)
        return true
    }

    fun refresh() {
        val dir = _state.value.dir
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            val entries = repo.listDir(dir, _state.value.prefs.showHidden)
            val ghosts = repo.ghostsFor(dir, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
            _state.value = _state.value.copy(entries = entries, ghosts = ghosts, loading = false)
        }
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun toggleSelect(path: String) {
        val sel = _state.value.selection.toMutableSet()
        if (!sel.add(path)) sel.remove(path)
        _state.value = _state.value.copy(selection = sel)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selection = emptySet())
    }

    fun selectAll() {
        _state.value = _state.value.copy(selection = _state.value.visibleEntries.map { it.path }.toSet())
    }

    fun stageClipboard(mode: ClipMode) {
        val sel = _state.value.selection.toList()
        if (sel.isEmpty()) return
        _state.value = _state.value.copy(clipboard = Clipboard(sel, mode), selection = emptySet())
    }

    fun cancelClipboard() {
        _state.value = _state.value.copy(clipboard = null)
    }

    fun paste() {
        val clip = _state.value.clipboard ?: return
        val dest = _state.value.dir
        viewModelScope.launch {
            val result = when (clip.mode) {
                ClipMode.COPY -> repo.copy(clip.paths, dest)
                ClipMode.MOVE -> repo.move(clip.paths, dest)
            }
            _state.value = _state.value.copy(clipboard = null, error = result.exceptionOrNull()?.message)
            refresh()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = repo.createFolder(_state.value.dir, name.ifBlank { "New folder" })
            _state.value = _state.value.copy(error = result.exceptionOrNull()?.message)
            refresh()
        }
    }

    fun rename(path: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val result = repo.rename(path, newName.trim())
            _state.value = _state.value.copy(error = result.exceptionOrNull()?.message)
            refresh()
        }
    }

    fun trashSelection() {
        val sel = _state.value.selection.toList()
        if (sel.isEmpty()) return
        viewModelScope.launch {
            val result = repo.moveToTrash(sel)
            _state.value = _state.value.copy(selection = emptySet(), error = result.exceptionOrNull()?.message)
            refresh()
        }
    }

    fun trash(path: String) {
        viewModelScope.launch {
            val result = repo.moveToTrash(listOf(path))
            _state.value = _state.value.copy(error = result.exceptionOrNull()?.message)
            refresh()
        }
    }

    fun setStarred(path: String, starred: Boolean) {
        viewModelScope.launch {
            repo.setStarred(path, starred)
            refresh()
        }
    }

    fun starSelection(starred: Boolean) {
        val sel = _state.value.selection.toList()
        viewModelScope.launch {
            sel.forEach { repo.setStarred(it, starred) }
            clearSelection()
            refresh()
        }
    }

    fun setSort(key: SortKey, dir: SortDir) {
        viewModelScope.launch { prefs.setSort(key, dir) }
    }

    fun setViewMode(mode: dev.madsens.geyma.data.ViewMode) {
        viewModelScope.launch { prefs.setViewMode(mode) }
    }

    fun setShowHidden(show: Boolean) {
        viewModelScope.launch { prefs.setShowHidden(show) }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    /** Record that a file was opened, so the seen-ledger stops flagging it as neglected. */
    fun noteOpened(path: String) {
        viewModelScope.launch { repo.recordOpen(path) }
    }

    companion object {
        fun factory(app: GeymaApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BrowserViewModel(app.repo, app.prefs) as T
        }
    }
}
