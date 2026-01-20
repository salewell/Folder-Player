package com.wing.folderplayer.utils

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val result = mutableListOf<LyricLine>()
        
        // Regex for [mm:ss.xx] or [mm:ss:xxx]
        val timeRegex = Regex("\\[(\\d{2,3}):(\\d{2})[.:](\\d{2,3})\\]")
        
        for (line in lines) {
            val match = timeRegex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                var ms = msStr.toLong()
                
                // Handle 2-digit vs 3-digit ms
                if (msStr.length == 2) ms *= 10
                
                val totalTime = min * 60000 + sec * 1000 + ms
                val text = line.substring(match.range.last + 1).trim()
                
                result.add(LyricLine(totalTime, text))
            }
        }
        
        return result.sortedBy { it.timeMs }
    }
}
