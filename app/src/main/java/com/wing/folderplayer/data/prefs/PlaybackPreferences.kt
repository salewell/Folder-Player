package com.wing.folderplayer.data.prefs

import android.content.Context
import com.wing.folderplayer.ui.browser.SourceConfig
import com.wing.folderplayer.utils.LyricLine
import com.google.gson.Gson

data class CachedMetadata(
    val title: String = "",
    val artist: String = "",
    val folderName: String = "",
    val audioInfo: String = "",
    val coverUri: String? = null,
    val lyrics: Array<LyricLine> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CachedMetadata
        if (title != other.title) return false
        if (!lyrics.contentEquals(other.lyrics)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + lyrics.contentHashCode()
        return result
    }
}

class PlaybackPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePlaybackState(source: SourceConfig?, folderPath: String?, mediaId: String?, position: Long) {
        prefs.edit().apply {
            putString("source_config", gson.toJson(source))
            putString("folder_path", folderPath)
            putString("last_media_id", mediaId)
            putLong("last_position", position)
            apply()
        }
    }

    fun getLastSourceConfig(): SourceConfig? {
        val json = prefs.getString("source_config", null) ?: return null
        return try { gson.fromJson(json, SourceConfig::class.java) } catch (e: Exception) { null }
    }

    fun getLastFolderPath(): String? = prefs.getString("folder_path", null)
    fun getLastMediaId(): String? = prefs.getString("last_media_id", null)
    fun getLastPosition(): Long = prefs.getLong("last_position", 0L)

    fun saveCachedMetadata(metadata: CachedMetadata) {
        prefs.edit().putString("cached_metadata", gson.toJson(metadata)).apply()
    }

    fun getCachedMetadata(): CachedMetadata? {
        val json = prefs.getString("cached_metadata", null) ?: return null
        return try { gson.fromJson(json, CachedMetadata::class.java) } catch (e: Exception) { null }
    }

    fun getCoverDisplaySize(): String = prefs.getString("cover_display_size", "STANDARD") ?: "STANDARD"
    fun saveCoverDisplaySize(size: String) {
        prefs.edit().putString("cover_display_size", size).apply()
    }

    fun getAutoNextFolder(): Boolean = prefs.getBoolean("auto_next_folder", false)
    fun saveAutoNextFolder(enabled: Boolean) {
        prefs.edit().putBoolean("auto_next_folder", enabled).apply()
    }

    fun savePosition(position: Long) {
        prefs.edit().putLong("last_position", position).apply()
    }

    fun getActivePlaylistId(): String = prefs.getString("active_playlist_id", "default") ?: "default"
    fun saveActivePlaylistId(id: String) {
        prefs.edit().putString("active_playlist_id", id).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
