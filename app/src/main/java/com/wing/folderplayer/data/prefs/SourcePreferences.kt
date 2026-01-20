package com.wing.folderplayer.data.prefs

import android.content.Context
import com.wing.folderplayer.ui.browser.SourceConfig
import com.wing.folderplayer.ui.browser.SourceType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SortConfig(
    val field: String,
    val ascending: Boolean
)

class SourcePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("source_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLastBrowsedState(source: SourceConfig?, path: String) {
        try {
            prefs.edit().apply {
                putString("last_source", gson.toJson(source))
                putString("last_path", path)
                apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("SourcePreferences", "Failed to save last browsed state", e)
        }
    }

    fun getLastBrowsedSource(): SourceConfig? {
        val json = prefs.getString("last_source", null) ?: return null
        return try { gson.fromJson(json, SourceConfig::class.java) } catch (e: Exception) { null }
    }

    fun getLastBrowsedPath(): String = prefs.getString("last_path", "ROOT") ?: "ROOT"

    fun getLastSource(): SourceConfig? = getLastBrowsedSource()
    fun getLastPath(): String = getLastBrowsedPath()

    fun getSources(): List<SourceConfig> {
        val json = prefs.getString("sources_list", null) ?: return emptyList()
        val type = object : TypeToken<List<SourceConfig>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun getSavedSources(): List<SourceConfig> = getSources()

    fun saveSources(sources: List<SourceConfig>) {
        try {
            prefs.edit().putString("sources_list", gson.toJson(sources)).apply()
        } catch (e: Exception) {
            android.util.Log.e("SourcePreferences", "Failed to save sources list", e)
        }
    }

    fun getDefaultSort(): SortOption {
        val field = prefs.getString("default_sort_field", "NAME") ?: "NAME"
        val asc = prefs.getBoolean("default_sort_asc", true)
        return SortOption(field, asc)
    }

    fun saveDefaultSort(field: String, ascending: Boolean) {
        prefs.edit().putString("default_sort_field", field).putBoolean("default_sort_asc", ascending).apply()
    }

    fun getDirectorySort(path: String): SortOption? {
        val field = prefs.getString("sort_field_$path", null) ?: return null
        val asc = prefs.getBoolean("sort_asc_$path", true)
        return SortOption(field, asc)
    }

    fun saveDirectorySort(path: String, field: String, ascending: Boolean) {
        prefs.edit().putString("sort_field_$path", field).putBoolean("sort_asc_$path", ascending).apply()
    }

    data class SortOption(val field: String, val ascending: Boolean)

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
