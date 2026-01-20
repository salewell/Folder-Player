package com.wing.folderplayer.ui.browser

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wing.folderplayer.data.source.LocalSource
import com.wing.folderplayer.data.source.MusicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.wing.folderplayer.data.source.MusicSource

import com.wing.folderplayer.data.source.WebDavSource
import com.wing.folderplayer.data.prefs.SourcePreferences
import java.util.UUID

import androidx.annotation.Keep

@Keep
enum class SourceType { LOCAL, WEBDAV }

@Keep
data class SourceConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: SourceType,
    val url: String = "",
    val path: String? = null,
    val username: String = "",
    val password: String = ""
)

data class CacheEntry(
    val files: List<MusicFile>,
    val timestamp: Long,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val sortField: String,
    val sortAscending: Boolean
)

data class BrowserUiState(
    val currentPath: String = "",
    val files: List<MusicFile> = emptyList(),
    val isLoading: Boolean = false,
    val currentSource: SourceConfig? = null,
    val currentlyPlayingPath: String? = null,
    val sortField: String = "NAME",
    val sortAscending: Boolean = true,
    val availableSources: List<SourceConfig> = listOf(
        SourceConfig(name = "Local Storage", type = SourceType.LOCAL, url = Environment.getExternalStorageDirectory().absolutePath)
    ),
    // Scroll memory
    val scrollToIndex: Int = 0,
    val scrollToOffset: Int = 0,
    val scrollTrigger: Int = 0, // Increment to trigger scroll in UI
    
    // Playlist Context Menu
    val selectedFileForPlaylist: MusicFile? = null
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("BrowserViewModel", "Unhandled exception in coroutine", throwable)
        _error.value = "An error occurred: ${throwable.localizedMessage}"
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    private val localSource = LocalSource()
    private val sourcePreferences: SourcePreferences
    // Cache WebDAV sources to avoid recreating
    private val webDavSources = mutableMapOf<String, WebDavSource>()
    
    // Directory Cache (Expires after 20 minutes)
    // Key is absolute path
    private val directoryCache = mutableMapOf<String, CacheEntry>()
    private val CACHE_EXPIRY_MS = 20 * 60 * 1000L // 20 minutes

    init {
        // Initialize prefs using application context
        sourcePreferences = com.wing.folderplayer.data.prefs.SourcePreferences(application)
        
        loadSources()
        
        // Initialize state, potentially restoring last browsed
        val lastSource = sourcePreferences.getLastBrowsedSource()
        val lastPath = sourcePreferences.getLastBrowsedPath()

        if (lastSource != null && lastPath != "ROOT") {
            if (lastSource.type == SourceType.WEBDAV) {
                com.wing.folderplayer.data.source.WebDavAuthManager.setCredentials(lastSource.username, lastSource.password)
            }
            _uiState.value = _uiState.value.copy(
                currentPath = lastPath,
                currentSource = lastSource
            )
            loadPath(lastPath)
        } else {
            _uiState.value = _uiState.value.copy(
                currentPath = "ROOT", 
                currentSource = null
            )
        }
    }

    private fun loadSources() {
        val context = getApplication<Application>()
        val savedSources = sourcePreferences.getSavedSources()
        
        val localSources = mutableListOf<SourceConfig>()
        localSources.add(SourceConfig(name = "Internal Storage", type = SourceType.LOCAL, url = Environment.getExternalStorageDirectory().absolutePath))
        
        // Discover SD Card
        val externalFilesDirs = context.getExternalFilesDirs(null)
        if (externalFilesDirs.size > 1) {
            for (i in 1 until externalFilesDirs.size) {
                val file = externalFilesDirs[i]
                if (file != null) {
                    val path = file.absolutePath.substringBefore("/Android")
                    localSources.add(SourceConfig(name = "SD Card ${if (i > 1) i else ""}", type = SourceType.LOCAL, url = path))
                }
            }
        }

        val allSources = localSources + savedSources
        _uiState.value = _uiState.value.copy(availableSources = allSources)
    }

    fun addWebDavSource(name: String, url: String, path: String?, user: String, pass: String) {
        val newSource = SourceConfig(
            name = name.ifBlank { "My NAS" },
            type = SourceType.WEBDAV,
            url = if (url.startsWith("http")) url else "http://$url",
            path = path,
            username = user,
            password = pass
        )
        val currentSources = sourcePreferences.getSavedSources()
        val updatedSources = currentSources + newSource
        sourcePreferences.saveSources(updatedSources)
        loadSources()
    }

    fun editWebDavSource(id: String, name: String, url: String, path: String?, user: String, pass: String) {
        val currentSources = sourcePreferences.getSavedSources().toMutableList()
        val index = currentSources.indexOfFirst { it.id == id }
        if (index != -1) {
            currentSources[index] = currentSources[index].copy(
                name = name,
                url = url,
                path = path,
                username = user,
                password = pass
            )
            sourcePreferences.saveSources(currentSources)
            webDavSources.remove(id) // Force recreate on next access
            loadSources()
        }
    }

    fun removeSource(sourceId: String) {
        val currentSources = sourcePreferences.getSavedSources().toMutableList()
        currentSources.removeAll { it.id == sourceId }
        sourcePreferences.saveSources(currentSources)
        webDavSources.remove(sourceId)
        loadSources()
    }

    fun duplicateWebDavSource(sourceId: String) {
        val currentSources = sourcePreferences.getSavedSources().toMutableList()
        val original = currentSources.find { it.id == sourceId }
        if (original != null) {
            val copy = original.copy(id = UUID.randomUUID().toString(), name = "${original.name} (Copy)")
            currentSources.add(copy)
            sourcePreferences.saveSources(currentSources)
            loadSources()
        }
    }

    fun moveSourceUp(sourceId: String) {
        val currentSources = sourcePreferences.getSavedSources().toMutableList()
        val index = currentSources.indexOfFirst { it.id == sourceId }
        if (index > 0) {
            val item = currentSources.removeAt(index)
            currentSources.add(index - 1, item)
            sourcePreferences.saveSources(currentSources)
            loadSources()
        }
    }

    fun moveSourceDown(sourceId: String) {
        val currentSources = sourcePreferences.getSavedSources().toMutableList()
        val index = currentSources.indexOfFirst { it.id == sourceId }
        if (index != -1 && index < currentSources.size - 1) {
            val item = currentSources.removeAt(index)
            currentSources.add(index + 1, item)
            sourcePreferences.saveSources(currentSources)
            loadSources()
        }
    }

    private fun getEffectiveSourcePath(config: SourceConfig): String {
        return when (config.type) {
            SourceType.LOCAL -> config.url
            SourceType.WEBDAV -> {
                var effectiveUrl = if (config.path?.isNotEmpty() == true) {
                    val baseUrl = config.url.trimEnd('/')
                    val path = config.path.trimStart('/')
                    "$baseUrl/$path"
                } else {
                    config.url
                }
            
                // Crucial: WebDAV directories usually MUST end with / to list contents correctly
                if (!effectiveUrl.endsWith("/")) {
                    effectiveUrl += "/"
                }
                
                // Basic encoding for spaces if not already encoded
                if (effectiveUrl.contains(" ") && !effectiveUrl.contains("%20")) {
                    effectiveUrl = effectiveUrl.replace(" ", "%20")
                }
                effectiveUrl
            }
        }
    }

    private fun getSource(config: SourceConfig): MusicSource {
        return when (config.type) {
            SourceType.LOCAL -> localSource
            SourceType.WEBDAV -> {
                webDavSources.getOrPut(config.id) {
                    WebDavSource(getEffectiveSourcePath(config), config.username, config.password)
                }
            }
        }
    }

    fun selectSource(sourceConfig: SourceConfig) {
        if (sourceConfig.type == SourceType.WEBDAV) {
            com.wing.folderplayer.data.source.WebDavAuthManager.setCredentials(sourceConfig.username, sourceConfig.password)
        } else {
            com.wing.folderplayer.data.source.WebDavAuthManager.clear()
        }
        _uiState.value = _uiState.value.copy(currentSource = sourceConfig)
        val path = getEffectiveSourcePath(sourceConfig)
        sourcePreferences.saveLastBrowsedState(sourceConfig, path)
        loadPath(path)
    }

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        directoryCache.remove(_uiState.value.currentPath)
        loadPath(_uiState.value.currentPath)
    }

    /**
     * @param isBackNavigation If true, we will try to restore the scroll position from cache.
     *                         If false, we will default to the top (0).
     */
    fun loadPath(path: String, isBackNavigation: Boolean = false) {
        val sourceConfig = _uiState.value.currentSource
        if (sourceConfig == null) {
             return
        }
        
        // Clean stale cache entries periodically
        val now = System.currentTimeMillis()
        directoryCache.entries.removeIf { now - it.value.timestamp > CACHE_EXPIRY_MS }

        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, currentPath = path)
                sourcePreferences.saveLastBrowsedState(_uiState.value.currentSource, path)
                
                // 1. Determine desired sort (Check override then global default)
                val override = sourcePreferences.getDirectorySort(path)
                val (field, asc) = if (override != null) {
                    override.field to override.ascending
                } else {
                    val default = sourcePreferences.getDefaultSort()
                    default.field to default.ascending
                }

                // 2. Check Cache
                val cached = directoryCache[path]
                if (cached != null && (now - cached.timestamp < CACHE_EXPIRY_MS)) {
                    val finalFiles = if (cached.sortField != field || cached.sortAscending != asc) {
                        applySort(cached.files, field, asc)
                    } else {
                        cached.files
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        files = finalFiles,
                        isLoading = false,
                        sortField = field,
                        sortAscending = asc,
                        scrollToIndex = if (isBackNavigation) cached.scrollIndex else 0,
                        scrollToOffset = if (isBackNavigation) cached.scrollOffset else 0,
                        scrollTrigger = _uiState.value.scrollTrigger + 1
                    )
                    return@launch
                }

                // 3. Fetch from Source
                val source = getSource(sourceConfig)
                val filesRaw = source.list(path)
                
                val files = applySort(filesRaw, field, asc)
                
                // 3. Update Cache
                directoryCache[path] = CacheEntry(
                    files = files,
                    timestamp = System.currentTimeMillis(),
                    sortField = field,
                    sortAscending = asc
                )
                
                _uiState.value = _uiState.value.copy(
                    files = files, 
                    isLoading = false,
                    sortField = field,
                    sortAscending = asc,
                    scrollToIndex = 0,
                    scrollToOffset = 0,
                    scrollTrigger = _uiState.value.scrollTrigger + 1
                )
            } catch (e: Exception) {
                android.util.Log.e("BrowserViewModel", "Error loading path: $path", e)
                _error.value = "Load Failed: ${e.localizedMessage ?: "Network or Server Error"}"
                _uiState.value = _uiState.value.copy(isLoading = false, files = emptyList())
            }
        }
    }

    fun saveScrollPosition(index: Int, offset: Int) {
        val currentPath = _uiState.value.currentPath
        directoryCache[currentPath]?.let { entry ->
            directoryCache[currentPath] = entry.copy(scrollIndex = index, scrollOffset = offset)
        }
    }

    private fun applySort(list: List<MusicFile>, by: String, ascending: Boolean): List<MusicFile> {
        // ALWAYS put folders first
        val (folders, files) = list.partition { it.isDirectory }
        
        val sortedFolders = when(by) {
            "NAME" -> if (ascending) folders.sortedBy { it.name.lowercase() } else folders.sortedByDescending { it.name.lowercase() }
            "DATE" -> if (ascending) folders.sortedBy { it.lastModified } else folders.sortedByDescending { it.lastModified }
            "SIZE" -> if (ascending) folders.sortedBy { it.size } else folders.sortedByDescending { it.size }
            else -> folders
        }
        
        val sortedFiles = when(by) {
            "NAME" -> if (ascending) files.sortedBy { it.name.lowercase() } else files.sortedByDescending { it.name.lowercase() }
            "DATE" -> if (ascending) files.sortedBy { it.lastModified } else files.sortedByDescending { it.lastModified }
            "SIZE" -> if (ascending) files.sortedBy { it.size } else files.sortedByDescending { it.size }
            else -> files
        }
        
        return sortedFolders + sortedFiles
    }

    fun updateCurrentlyPlaying(path: String?) {
        _uiState.value = _uiState.value.copy(currentlyPlayingPath = path)
    }

    fun sortFiles(by: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentField = _uiState.value.sortField
                val currentAscending = _uiState.value.sortAscending
                val currentPath = _uiState.value.currentPath
                val filesToSort = _uiState.value.files
                
                // Toggle if same field, default to ascending if new field
                val newAscending = if (currentField == by) !currentAscending else true
                
                val sorted = withContext(kotlinx.coroutines.Dispatchers.Default) {
                     applySort(filesToSort, by, newAscending)
                }
                
                // Save as override for this folder
                try {
                    if (currentPath != "ROOT" && currentPath.isNotEmpty()) {
                        sourcePreferences.saveDirectorySort(currentPath, by, newAscending)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BrowserViewModel", "Error saving dir sort", e)
                }
                
                _uiState.value = _uiState.value.copy(files = sorted, sortField = by, sortAscending = newAscending)

                // Update Cache to reflect new sorted state
                directoryCache[currentPath]?.let { entry ->
                    directoryCache[currentPath] = entry.copy(
                        files = sorted,
                        sortField = by,
                        sortAscending = newAscending
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("BrowserViewModel", "Error sorting files", e)
            }
        }
    }

    fun navigateUp() {
        val currentSource = _uiState.value.currentSource
        if (currentSource == null) return

        val path = _uiState.value.currentPath
        val effectiveRoot = getEffectiveSourcePath(currentSource)
        
        if (currentSource.type == SourceType.LOCAL) {
             // Local logic
             if (path == effectiveRoot) {
                 // Back to Root
                 exitSource()
             } else {
                  val parent = java.io.File(path).parent
                  if (parent != null && parent.startsWith(effectiveRoot)) { 
                      loadPath(parent, isBackNavigation = true)
                  } else {
                     exitSource()
                 }
             }
        } else {
            // WebDAV Logic
            val pathClean = path.trimEnd('/')
            val rootClean = effectiveRoot.trimEnd('/')
            
            if (pathClean == rootClean || path.isEmpty() || path == "/") {
                exitSource()
            } else {
                 val parentPath = path.trimEnd('/').substringBeforeLast('/', "")
                 
                 if (parentPath.length < rootClean.length || !parentPath.startsWith("http")) {
                     exitSource()
                  } else {
                       loadPath(parentPath, isBackNavigation = true)
                  }
            }
        }
    }

    fun exitSource() {
        sourcePreferences.saveLastBrowsedState(null, "ROOT")
        _uiState.value = _uiState.value.copy(currentSource = null, currentPath = "ROOT", files = emptyList())
    }

    fun onFileClicked(
        file: MusicFile, 
        onFolderPlay: (SourceConfig, String, String?) -> Unit,
        onCustomPlay: (SourceConfig, List<MusicFile>, Int) -> Unit,
        onCuePlay: (SourceConfig, String) -> Unit
    ) {
        if (file.isDirectory) {
            loadPath(file.path)
        } else {
             val ext = file.name.substringAfterLast('.', "").lowercase()
             
             // Handle CUE Specifically
             if (ext == "cue") {
                 _uiState.value.currentSource?.let { sourceConfig ->
                     onCuePlay(sourceConfig, file.path)
                 }
                 return
             }

             // Filter: Only play music files
             if (!isMusic(file.name)) return

             _uiState.value.currentSource?.let { sourceConfig ->
                 // Logic to play folder while maintaining SORT ORDER
                 val musicFiles = _uiState.value.files.filter { !it.isDirectory && isMusic(it.name) }
                 val clickIndex = musicFiles.indexOfFirst { it.path == file.path }
                 
                 if (clickIndex != -1) {
                     onCustomPlay(sourceConfig, musicFiles, clickIndex)
                 } else {
                     // Fallback
                     val parentPath = if (sourceConfig.type == SourceType.LOCAL) {
                         java.io.File(file.path).parent ?: file.path
                     } else {
                         if (file.path.contains('/')) {
                            file.path.substringBeforeLast('/')
                         } else {
                            file.path
                         }
                     }
                     onFolderPlay(sourceConfig, parentPath, file.path)
                 }
             }
        }
    }
    
    fun playCurrentFolder(onFolderPlay: (SourceConfig, String, String?) -> Unit) {
        _uiState.value.currentSource?.let {
            onFolderPlay(it, _uiState.value.currentPath, null)
        }
    }

    private fun isMusic(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        // Specifically exclude playlist/container files from the music list
        return ext in listOf("mp3", "flac", "m4a", "wav", "ogg", "aac", "opus", "ape", "dsf", "dff")
    }

    fun shufflePlay(onFolderPlay: (SourceConfig, String, String?) -> Unit, onCustomPlay: (SourceConfig, List<MusicFile>, Int) -> Unit) {
        val source = _uiState.value.currentSource ?: return
        val files = _uiState.value.files
        if (files.isEmpty()) return

        val musicFiles = files.filter { !it.isDirectory && isMusic(it.name) }
        val folders = files.filter { it.isDirectory }

        if (musicFiles.isNotEmpty()) {
            // Shuffle music files and play
            val shuffled = musicFiles.shuffled()
            onCustomPlay(source, shuffled, 0)
        } else if (folders.isNotEmpty()) {
            // Pick a random folder and play it sequential
            val picked = folders.random()
            onFolderPlay(source, picked.path, null)
        }
    }

    fun onFileLongClick(file: MusicFile) {
        _uiState.value = _uiState.value.copy(selectedFileForPlaylist = file)
    }

    fun onAddToPlaylistDone() {
        _uiState.value = _uiState.value.copy(selectedFileForPlaylist = null)
    }

    fun closePlaylistDialog() {
        _uiState.value = _uiState.value.copy(selectedFileForPlaylist = null)
    }
}
