package com.wing.folderplayer.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log

/**
 * Lightweight embedded lyrics extractor using Android's native MediaMetadataRetriever
 * Note: Support is very limited - only works for some MP3 files with specific tags
 */
object EmbeddedLyricsExtractor {
    
    private const val TAG = "EmbeddedLyricsExtractor"
    
    // Custom metadata key for lyrics (not standard, rarely supported)
    private const val METADATA_KEY_LYRICS = 1000
    
    /**
     * Extract embedded lyrics from a local audio file
     * Uses Android's built-in MediaMetadataRetriever
     * 
     * WARNING: This has very limited support and will fail for most files!
     * Recommend using external .lrc files instead.
     */
    suspend fun extractFromFile(filePath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            
            // Try to extract lyrics metadata
            // Note: This will fail for most files as Android's MediaMetadataRetriever
            // has very limited support for lyrics tags
            val lyrics = try {
                retriever.extractMetadata(METADATA_KEY_LYRICS)
            } catch (e: Exception) {
                null
            }
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "Found embedded lyrics in $filePath")
                return lyrics
            }
            
            Log.d(TAG, "No embedded lyrics found in $filePath (this is normal)")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting lyrics from $filePath", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }
    
    /**
     * Check if a file might support embedded lyrics based on extension
     * Note: Even these formats rarely have extractable lyrics via Android API
     */
    fun supportsEmbeddedLyrics(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        // Focus on formats that MediaMetadataRetriever handles best
        return when (extension) {
            "mp3" -> true  // Best (but still limited) support
            "m4a", "mp4" -> true  // Limited support
            else -> false  // Everything else: no support
        }
    }
}
