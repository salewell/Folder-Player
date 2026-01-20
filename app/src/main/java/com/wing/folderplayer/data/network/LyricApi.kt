package com.wing.folderplayer.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

object LyricApi {
    private val client = OkHttpClient()

    suspend fun fetchLyrics(apiUrl: String, title: String, artist: String?): String? = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist ?: "", "UTF-8")
            
            // Construct URL. Assuming standard query params. 
            // If the user's API is different, they can change the base URL in settings.
            val url = if (apiUrl.contains("?")) {
                "$apiUrl&title=$encodedTitle&artist=$encodedArtist"
            } else {
                "$apiUrl?title=$encodedTitle&artist=$encodedArtist"
            }

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LyricApi", "Error fetching lyrics", e)
            null
        }
    }
}
