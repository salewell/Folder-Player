package com.wing.folderplayer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wing.folderplayer.ui.browser.BrowserScreen
import com.wing.folderplayer.ui.player.MainPlayerScreen
import com.wing.folderplayer.ui.player.PlayerViewModel
import com.wing.folderplayer.ui.settings.SettingsScreen
import com.wing.folderplayer.ui.theme.FolderPlayerTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        // Request Permissions
        val permissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { _ -> 
        }

//        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
//        val decorView = getWindow().getDecorView()
//        decorView.setSystemUiVisibility(
//            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        )


        val permissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        permissionLauncher.launch(permissions)


        setContent {
            FolderPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Red) {
                    val playerViewModel: PlayerViewModel = viewModel() 
                    val browserViewModel: com.wing.folderplayer.ui.browser.BrowserViewModel = viewModel()
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    val scope = rememberCoroutineScope()

                    // Bridge State: Update browser's "playing" indicator when player state changes
                    androidx.compose.runtime.LaunchedEffect(playerViewModel.uiState) {
                        playerViewModel.uiState.collect { state ->
                            val path = state.currentMediaId?.let { 
                                if (it.startsWith("file://")) android.net.Uri.parse(it).path else it
                            }
                            browserViewModel.updateCurrentlyPlaying(path)
                        }
                    }

                    HorizontalPager(state = pagerState) { page ->
                        when (page) {
                            0 -> MainPlayerScreen(
                                viewModel = playerViewModel
                            )
                            1 -> BrowserScreen(
                                viewModel = browserViewModel,
                                onFolderPlay = { sourceConfig, path, startingFileUri ->
                                    playerViewModel.playFolder(sourceConfig, path, startingFileUri)
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                },
                                onCustomPlay = { sourceConfig, files, index ->
                                    playerViewModel.playCustomList(sourceConfig, files, index)
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                },
                                onCuePlay = { sourceConfig, cuePath ->
                                    playerViewModel.playCueSheet(sourceConfig, cuePath)
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                },
                                allPlaylists = playerViewModel.uiState.collectAsState().value.allPlaylists,
                                onAddToPlaylist = { targetListId, musicFiles ->
                                    val currentSource = browserViewModel.uiState.value.currentSource
                                    if (currentSource != null) {
                                        playerViewModel.addFilesToPlaylist(targetListId, currentSource, musicFiles)
                                    }
                                },
                                onBack = { 
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                }
                            )
                            2 -> {
                                val state by playerViewModel.uiState.collectAsState()
                                SettingsScreen(
                                    onBack = { 
                                        scope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    currentCoverSize = state.coverDisplaySize,
                                    onCoverSizeChange = { playerViewModel.setCoverDisplaySize(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
