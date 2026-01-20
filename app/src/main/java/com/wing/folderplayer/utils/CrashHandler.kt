package com.wing.folderplayer.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {

    private const val DEBUG_PREF_KEY = "debug_mode_enabled"
    private const val PREFS_NAME = "app_debug_prefs"

    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDebugEnabled = prefs.getBoolean(DEBUG_PREF_KEY, false)

        if (isDebugEnabled) {
            setupExceptionHandler()
        }
    }

    fun isDebugEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(DEBUG_PREF_KEY, false)
    }

    fun setDebugEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(DEBUG_PREF_KEY, enabled).apply()
        
        if (enabled) {
            setupExceptionHandler()
        }
    }

    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleException(thread, throwable)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun handleException(thread: Thread, throwable: Throwable) {
        val context = applicationContext ?: return
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val fileName = "FolderPlayer_Crash_${System.currentTimeMillis()}.txt"

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val logContent = StringBuilder()
        logContent.append("=== Folder Player Crash Log ===\n")
        logContent.append("Time: $timestamp\n")
        logContent.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        logContent.append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        logContent.append("Thread: ${thread.name}\n\n")
        logContent.append("Exception:\n$stackTrace\n")
        logContent.append("===============================\n")

        // Strategy 1: Try MediaStore (Android 10+ / Public Downloads)
        // This makes files visible to user without Root
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FolderPlayer_Logs")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(logContent.toString().toByteArray())
                    }
                    return // Success
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallthrough to Strategy 2
            }
        } else {
            // Strategy 1b: Legacy Public Storage (Android 9 and below)
            try {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val logDir = File(publicDir, "FolderPlayer_Logs")
                if (!logDir.exists()) logDir.mkdirs()
                
                val file = File(logDir, fileName)
                file.writeText(logContent.toString())
                return // Success
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallthrough to Strategy 2
            }
        }

        // Strategy 2: Fallback to Private App Storage (Reliable but hidden)
        try {
            val logDir = context.getExternalFilesDir("CrashLogs")
            if (logDir != null) {
                if (!logDir.exists()) logDir.mkdirs()
                val file = File(logDir, fileName)
                file.writeText(logContent.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
