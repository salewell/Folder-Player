package com.wing.folderplayer.utils

data class CueTrack(
    val number: Int,
    val title: String,
    val performer: String?,
    val startTimeMs: Long,
    var endTimeMs: Long? = null
)

object CueParser {
    fun parse(cueContent: String): Pair<String?, List<CueTrack>> {
        val tracks = mutableListOf<CueTrack>()
        var globalPerformer: String? = null
        var referencedFile: String? = null
        
        var currentTrackNumber: Int? = null
        var currentTitle: String? = null
        var currentPerformer: String? = null
        var currentStartTime: Long? = null

        val lines = cueContent.lines().map { it.trim() }

        for (line in lines) {
            when {
                line.startsWith("FILE", ignoreCase = true) -> {
                    // Format: FILE "filename" BINARY/WAVE/etc
                    referencedFile = line.substringAfter("FILE").trim()
                        .substringBeforeLast(" ") // Remove BINARY/WAVE
                        .trim().removeSurrounding("\"")
                }
                line.startsWith("PERFORMER", ignoreCase = true) -> {
                    val performer = line.substringAfter("PERFORMER").trim().removeSurrounding("\"")
                    if (currentTrackNumber == null) {
                        globalPerformer = performer
                    } else {
                        currentPerformer = performer
                    }
                }
                line.startsWith("TRACK", ignoreCase = true) -> {
                    // Save previous track if exists
                    if (currentTrackNumber != null && currentStartTime != null) {
                        tracks.add(CueTrack(
                            currentTrackNumber,
                            currentTitle ?: "Track $currentTrackNumber",
                            currentPerformer ?: globalPerformer,
                            currentStartTime
                        ))
                    }
                    
                    val parts = line.split(Regex("\\s+"))
                    currentTrackNumber = parts.getOrNull(1)?.toIntOrNull()
                    currentTitle = null
                    currentPerformer = null
                    currentStartTime = null
                }
                line.startsWith("TITLE", ignoreCase = true) -> {
                    currentTitle = line.substringAfter("TITLE").trim().removeSurrounding("\"")
                }
                line.startsWith("INDEX 01", ignoreCase = true) -> {
                    val timeStr = line.substringAfter("INDEX 01").trim()
                    currentStartTime = parseCueTime(timeStr)
                }
            }
        }

        // Add the last track
        if (currentTrackNumber != null && currentStartTime != null) {
            tracks.add(CueTrack(
                currentTrackNumber,
                currentTitle ?: "Track $currentTrackNumber",
                currentPerformer ?: globalPerformer,
                currentStartTime
            ))
        }

        // Calculate end times
        for (i in 0 until tracks.size - 1) {
            tracks[i].endTimeMs = tracks[i+1].startTimeMs
        }

        return Pair(referencedFile, tracks)
    }

    private fun parseCueTime(time: String): Long {
        // Format: MM:SS:FF (Minutes:Seconds:Frames, 1 second = 75 frames)
        val parts = time.split(':')
        if (parts.size != 3) return 0L
        
        val min = parts[0].toLongOrNull() ?: 0L
        val sec = parts[1].toLongOrNull() ?: 0L
        val frames = parts[2].toLongOrNull() ?: 0L
        
        return (min * 60 * 1000) + (sec * 1000) + (frames * 1000 / 75)
    }
}
