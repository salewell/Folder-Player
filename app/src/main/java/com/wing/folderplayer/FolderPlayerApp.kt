package com.wing.folderplayer

import android.app.Application
import android.content.Context

class FolderPlayerApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        com.wing.folderplayer.utils.CrashHandler.init(this)
    }
}
