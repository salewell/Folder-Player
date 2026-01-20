package com.wing.folderplayer.data.playlist

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wing.folderplayer.ui.browser.SourceConfig
import java.io.File

data class PlaylistItem(
    val path: String,
    val title: String,
    val artist: String?,
    val sourceId: String, // Matches SourceConfig.id or a unique key
    val artworkUri: String? = null,
    val durationMs: Long = 0
)

data class Playlist(
    val id: String, // Filename e.g. "default", "list_1"
    val name: String,
    val items: List<PlaylistItem> = emptyList()
)

class PlaylistManager(private val context: Context) {
    private val gson = Gson()
    private val playlistsDir = File(context.filesDir, "playlists").apply { if (!exists()) mkdirs() }
    private val metadataFile = File(playlistsDir, "metadata.json")

    // Metadata keeps track of display names for IDs
    private var playlistMetadata: MutableMap<String, String> = mutableMapOf("default" to "Default")

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        if (metadataFile.exists()) {
            try {
                val json = metadataFile.readText()
                val type = object : TypeToken<MutableMap<String, String>>() {}.type
                playlistMetadata = gson.fromJson(json, type) ?: mutableMapOf("default" to "Default")
                // Ensure default always exists
                if (!playlistMetadata.containsKey("default")) {
                    playlistMetadata["default"] = "Default"
                }
            } catch (e: Exception) {
                playlistMetadata = mutableMapOf("default" to "Default")
            }
        }
    }

    private fun saveMetadata() {
        metadataFile.writeText(gson.toJson(playlistMetadata))
    }

    fun getAllPlaylists(): List<Playlist> {
        return playlistMetadata.map { (id, name) ->
            getPlaylist(id) ?: Playlist(id, name)
        }
    }

    fun getPlaylist(id: String): Playlist? {
        val file = File(playlistsDir, "$id.fpl")
        if (!file.exists()) return null
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<PlaylistItem>>() {}.type
            val items: List<PlaylistItem> = gson.fromJson(json, type) ?: emptyList()
            Playlist(id, playlistMetadata[id] ?: id, items)
        } catch (e: Exception) {
            null
        }
    }

    fun savePlaylist(playlist: Playlist) {
        val file = File(playlistsDir, "${playlist.id}.fpl")
        file.writeText(gson.toJson(playlist.items))
        playlistMetadata[playlist.id] = playlist.name
        saveMetadata()
    }

    fun createPlaylist(name: String): String? {
        if (playlistMetadata.size >= 11) return null
        val id = "list_${System.currentTimeMillis()}"
        playlistMetadata[id] = name
        savePlaylist(Playlist(id, name, emptyList()))
        return id
    }

    fun renamePlaylist(id: String, newName: String) {
        if (id == "default") return // Cannot rename default
        playlistMetadata[id] = newName
        saveMetadata()
    }

    fun deletePlaylist(id: String) {
        if (id == "default") return
        val file = File(playlistsDir, "$id.fpl")
        if (file.exists()) file.delete()
        playlistMetadata.remove(id)
        saveMetadata()
    }
    
    fun appendToPlaylist(id: String, newItem: PlaylistItem) {
        val p = getPlaylist(id) ?: Playlist(id, playlistMetadata[id] ?: id)
        val newItems = p.items.toMutableList().apply { add(newItem) }
        savePlaylist(p.copy(items = newItems))
    }

    fun appendToPlaylist(id: String, newItems: List<PlaylistItem>) {
        val p = getPlaylist(id) ?: Playlist(id, playlistMetadata[id] ?: id)
        val updatedItems = p.items.toMutableList().apply { addAll(newItems) }
        savePlaylist(p.copy(items = updatedItems))
    }
    
    fun removeFromPlaylist(id: String, index: Int) {
        val p = getPlaylist(id) ?: return
        if (index in p.items.indices) {
            val newItems = p.items.toMutableList().apply { removeAt(index) }
            savePlaylist(p.copy(items = newItems))
        }
    }
}
