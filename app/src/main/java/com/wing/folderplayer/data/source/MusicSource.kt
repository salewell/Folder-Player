package com.wing.folderplayer.data.source

import android.net.Uri

interface MusicSource {
    suspend fun list(path: String): List<MusicFile>
    fun getUri(path: String): Uri
    suspend fun readText(path: String): String?
}
