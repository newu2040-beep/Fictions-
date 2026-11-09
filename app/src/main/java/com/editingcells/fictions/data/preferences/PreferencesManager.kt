package com.editingcells.fictions.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.editingcells.fictions.domain.model.PastelTheme
import com.editingcells.fictions.domain.model.SortOption
import com.editingcells.fictions.domain.model.ThemeMode
import com.editingcells.fictions.domain.model.UserPreferences
import com.editingcells.fictions.domain.model.ViewMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fictions_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PASTEL_THEME = stringPreferencesKey("pastel_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
        val DEFAULT_SORT_OPTION = stringPreferencesKey("default_sort_option")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val READER_FONT_SIZE = floatPreferencesKey("reader_font_size")
        val DEMO_DATA_CLEANED = booleanPreferencesKey("demo_data_cleaned_v2")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val theme = prefs[Keys.PASTEL_THEME]?.let {
                runCatching { PastelTheme.valueOf(it) }.getOrNull()
            } ?: PastelTheme.LAVENDER

            val themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM

            val viewMode = prefs[Keys.DEFAULT_VIEW_MODE]?.let {
                runCatching { ViewMode.valueOf(it) }.getOrNull()
            } ?: ViewMode.GRID

            val sortOption = prefs[Keys.DEFAULT_SORT_OPTION]?.let {
                runCatching { SortOption.valueOf(it) }.getOrNull()
            } ?: SortOption.DATE

            val sortAscending = prefs[Keys.SORT_ASCENDING] ?: false
            val readerFontSize = prefs[Keys.READER_FONT_SIZE] ?: 16f

            UserPreferences(
                theme = theme,
                themeMode = themeMode,
                defaultViewMode = viewMode,
                defaultSortOption = sortOption,
                sortAscending = sortAscending,
                readerFontSizeSp = readerFontSize.coerceIn(12f, 30f)
            )
        }

    suspend fun isDemoDataCleaned(): Boolean {
        val prefs = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
        return prefs[Keys.DEMO_DATA_CLEANED] ?: false
    }

    suspend fun markDemoDataCleaned() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEMO_DATA_CLEANED] = true
        }
    }

    suspend fun setTheme(theme: PastelTheme) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PASTEL_THEME] = theme.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDefaultViewMode(viewMode: ViewMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_VIEW_MODE] = viewMode.name
        }
    }

    suspend fun setDefaultSortOption(sortOption: SortOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_SORT_OPTION] = sortOption.name
        }
    }

    suspend fun setSortAscending(ascending: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SORT_ASCENDING] = ascending
        }
    }

    suspend fun setReaderFontSize(sizeSp: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.READER_FONT_SIZE] = sizeSp.coerceIn(12f, 30f)
        }
    }
}
