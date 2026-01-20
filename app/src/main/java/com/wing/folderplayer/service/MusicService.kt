package com.wing.folderplayer.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.wing.folderplayer.MainActivity
import com.wing.folderplayer.data.repo.PlayerRepository
import com.wing.folderplayer.data.source.LocalSource
import okhttp3.OkHttpClient
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieHandler
import java.util.concurrent.TimeUnit

class MusicService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    // In a real app, inject this
    private val repository = PlayerRepository()
    private val localSource = LocalSource()
    
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // For cloud storage that may require cookies (e.g., some redirected links)
        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        CookieHandler.setDefault(cookieManager)

        // Create a robust OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cookieJar(okhttp3.JavaNetCookieJar(cookieManager))
            .build()

        // Create OkHttpDataSource.Factory
        val okHttpDataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 13; MCloudApp/10.7.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

        // Wrap it to dynamically add header on every request
        val resolvingHttpDataSourceFactory = androidx.media3.datasource.DataSource.Factory {
            val dataSource = okHttpDataSourceFactory.createDataSource()
            val auth = com.wing.folderplayer.data.source.WebDavAuthManager.authHeader
            
            if (auth != null) {
                // Alist specific: Basic auth for original server only
                // Media3/OkHttp will NOT pass this to different domains after redirect
                dataSource.setRequestProperty("Authorization", auth)
            }
            
            dataSource.setRequestProperty("Accept", "audio/*, */*")
            dataSource.setRequestProperty("Cache-Control", "no-cache")
            
            dataSource
        }
        
        // Use DefaultDataSource.Factory which handles file://, content://, etc., and plays nice with our custom Http factory
        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, resolvingHttpDataSourceFactory)

        // Configure LoadControl with larger buffers for smoother network streaming
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000, // Min buffer 30s
                60000, // Max buffer 60s
                2500,  // Buffer for playback 2.5s
                5000   // Buffer for playback after rebuffer 5s
            )
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(this)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(defaultDataSourceFactory)
            )
            .setLoadControl(loadControl)
            .build()
        
        player.volume = 1.0f
        

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicService", "Player Error: ${error.errorCodeName} (${error.errorCode})", error)
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    // Try to recover or just log
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when(playbackState) {
                    androidx.media3.common.Player.STATE_BUFFERING -> android.util.Log.d("MusicService", "Buffering...")
                    androidx.media3.common.Player.STATE_READY -> android.util.Log.d("MusicService", "Ready to play")
                    androidx.media3.common.Player.STATE_ENDED -> android.util.Log.d("MusicService", "Playback ended")
                    else -> {}
                }
            }
        })
        
        mediaSession = MediaLibrarySession.Builder(this, player, object : MediaLibraryService.MediaLibrarySession.Callback {
            // Implement simple callback if needed, or leave default for now
        })
            .setSessionActivity(pendingIntent)
            .build()

        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        unregisterReceiver(becomingNoisyReceiver)
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
