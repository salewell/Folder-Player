package com.wing.folderplayer.data.source

import android.net.Uri
import java.io.File

class LocalSource : MusicSource {
    override suspend fun list(path: String): List<MusicFile> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        
        return dir.listFiles()?.map {
            MusicFile(
                name = it.name,
                path = it.absolutePath,
                isDirectory = it.isDirectory,
                size = it.length(),
                lastModified = it.lastModified()
            )
        } ?: emptyList()
    }

    override fun getUri(path: String): Uri {
        return Uri.fromFile(File(path))
    }

    override suspend fun readText(path: String): String? {
        val file = File(path)
        return if (file.exists() && file.isFile) {
            try {
                file.readText()
            } catch (e: Exception) {
                null
            }
        } else null
    }
}
