package dev.madsens.geyma.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.madsens.geyma.theme.BackgroundPattern
import dev.madsens.geyma.theme.FontKey
import dev.madsens.geyma.theme.ResolvedTheme
import dev.madsens.geyma.theme.SkinOverrides
import dev.madsens.geyma.theme.TileStyle
import dev.madsens.geyma.theme.resolveTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "geyma_prefs")

enum class ViewMode { LIST, GRID }
enum class SortKey { NAME, KIND, SIZE, MODIFIED }
enum class SortDir { ASC, DESC }

data class ViewPrefs(
    val viewMode: ViewMode = ViewMode.LIST,
    val sortKey: SortKey = SortKey.NAME,
    val sortDir: SortDir = SortDir.ASC,
    val showHidden: Boolean = false,
)

class Prefs(private val context: Context) {

    private object Keys {
        val skin = stringPreferencesKey("skin")
        val accent = longPreferencesKey("ov_accent")
        val font = stringPreferencesKey("ov_font")
        val radius = intPreferencesKey("ov_radius")
        val tile = stringPreferencesKey("ov_tile")
        val iconMono = booleanPreferencesKey("ov_icon_mono")
        val pattern = stringPreferencesKey("ov_pattern")
        val viewMode = stringPreferencesKey("view_mode")
        val sortKey = stringPreferencesKey("sort_key")
        val sortDir = stringPreferencesKey("sort_dir")
        val showHidden = booleanPreferencesKey("show_hidden")
        val arrivalsSeeded = booleanPreferencesKey("arrivals_seeded")
    }

    /** False until Geyma has taken its first inventory of the watched folders. */
    val arrivalsSeeded: Flow<Boolean> = context.dataStore.data.map { it[Keys.arrivalsSeeded] ?: false }

    suspend fun setArrivalsSeeded(seeded: Boolean) =
        context.dataStore.edit { it[Keys.arrivalsSeeded] = seeded }

    val theme: Flow<ResolvedTheme> = context.dataStore.data.map { p ->
        val ov = SkinOverrides(
            accent = p[Keys.accent]?.let { Color(it) },
            fontKey = p[Keys.font]?.let { runCatching { FontKey.valueOf(it) }.getOrNull() },
            radius = p[Keys.radius],
            tile = p[Keys.tile]?.let { runCatching { TileStyle.valueOf(it) }.getOrNull() },
            iconMono = p[Keys.iconMono],
            pattern = p[Keys.pattern]?.let { runCatching { BackgroundPattern.valueOf(it) }.getOrNull() },
        )
        resolveTheme(p[Keys.skin] ?: "obsidian", ov)
    }

    val viewPrefs: Flow<ViewPrefs> = context.dataStore.data.map { p ->
        ViewPrefs(
            viewMode = p[Keys.viewMode]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.LIST,
            sortKey = p[Keys.sortKey]?.let { runCatching { SortKey.valueOf(it) }.getOrNull() } ?: SortKey.NAME,
            sortDir = p[Keys.sortDir]?.let { runCatching { SortDir.valueOf(it) }.getOrNull() } ?: SortDir.ASC,
            showHidden = p[Keys.showHidden] ?: false,
        )
    }

    suspend fun setSkin(id: String) = context.dataStore.edit { it[Keys.skin] = id }

    suspend fun setAccent(color: Color?) = context.dataStore.edit {
        if (color == null) it.remove(Keys.accent) else it[Keys.accent] = (color.value shr 32).toLong()
    }

    suspend fun setFont(font: FontKey?) = context.dataStore.edit {
        if (font == null) it.remove(Keys.font) else it[Keys.font] = font.name
    }

    suspend fun setRadius(radius: Int?) = context.dataStore.edit {
        if (radius == null) it.remove(Keys.radius) else it[Keys.radius] = radius
    }

    suspend fun setTile(tile: TileStyle?) = context.dataStore.edit {
        if (tile == null) it.remove(Keys.tile) else it[Keys.tile] = tile.name
    }

    suspend fun setIconMono(mono: Boolean?) = context.dataStore.edit {
        if (mono == null) it.remove(Keys.iconMono) else it[Keys.iconMono] = mono
    }

    suspend fun setPattern(pattern: BackgroundPattern?) = context.dataStore.edit {
        if (pattern == null) it.remove(Keys.pattern) else it[Keys.pattern] = pattern.name
    }

    suspend fun resetOverrides() = context.dataStore.edit {
        it.remove(Keys.accent); it.remove(Keys.font); it.remove(Keys.radius)
        it.remove(Keys.tile); it.remove(Keys.iconMono); it.remove(Keys.pattern)
    }

    suspend fun setViewMode(mode: ViewMode) = context.dataStore.edit { it[Keys.viewMode] = mode.name }
    suspend fun setSort(key: SortKey, dir: SortDir) = context.dataStore.edit {
        it[Keys.sortKey] = key.name
        it[Keys.sortDir] = dir.name
    }
    suspend fun setShowHidden(show: Boolean) = context.dataStore.edit { it[Keys.showHidden] = show }
}
