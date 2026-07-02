package com.example

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.MainPlayerViewModel
import com.example.ui.components.TVPlayerLayout
import com.example.ui.components.TVScreensaverLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvBgDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    
    private var playerViewModel: MainPlayerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                playerViewModel = viewModel

                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // Observe Room DB soundtrack playlist
                val songsList by viewModel.songsList.collectAsState()
                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val elapsedTimeMs by viewModel.elapsedTimeMs.collectAsState()
                val volume by viewModel.volume.collectAsState()
                val loopMode by viewModel.loopMode.collectAsState()
                val isShuffle by viewModel.isShuffle.collectAsState()
                val isScreensaverActive by viewModel.isScreensaverActive.collectAsState()

                // Observe casting states
                val isTvReceiverMode by viewModel.isTvReceiverMode.collectAsState()
                val tvReceiverIp by viewModel.tvReceiverIp.collectAsState()
                val isCastingActive by viewModel.isCastingActive.collectAsState()
                val targetTvIp by viewModel.targetTvIp.collectAsState()
                val castStatusMessage by viewModel.castStatusMessage.collectAsState()

                // File Picker Launcher to choose multiple audio files from device
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetMultipleContents()
                ) { uris: List<Uri> ->
                    if (uris.isNotEmpty()) {
                        coroutineScope.launch {
                            uris.forEach { uri ->
                                val meta = getAudioFileMetadata(context, uri)
                                viewModel.importAudioFile(
                                    uriString = meta.localPath ?: uri.toString(),
                                    fileName = meta.fileName,
                                    durationMs = meta.durationMs
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TvBgDark
                ) {
                    if (isScreensaverActive) {
                        TVScreensaverLayout(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            elapsedTimeMs = elapsedTimeMs
                        )
                    } else {
                        TVPlayerLayout(
                            songsList = songsList,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            elapsedTimeMs = elapsedTimeMs,
                            volume = volume,
                            loopMode = loopMode,
                            isShuffle = isShuffle,
                            isTvReceiverMode = isTvReceiverMode,
                            tvReceiverIp = tvReceiverIp,
                            isCastingActive = isCastingActive,
                            targetTvIp = targetTvIp,
                            castStatusMessage = castStatusMessage,
                            onStartTvReceiver = { viewModel.startTvReceiver() },
                            onStopTvReceiver = { viewModel.stopTvReceiver() },
                            onConnectToTv = { viewModel.connectToTv(it) },
                            onDisconnectCast = { viewModel.disconnectCast() },
                            onSongSelect = { viewModel.selectSong(it) },
                            onPlayPauseToggle = { viewModel.togglePlayPause() },
                            onNextSong = { viewModel.nextSong() },
                            onPreviousSong = { viewModel.previousSong() },
                            onAdjustVolume = { viewModel.adjustVolume(it) },
                            onToggleLoop = { viewModel.toggleLoopMode() },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onLoadSamples = { viewModel.loadSampleSongs() },
                            onClearPlaylist = { viewModel.clearPlaylist() },
                            onImportLocalFiles = {
                                viewModel.resetInactivityTimer()
                                filePickerLauncher.launch("audio/*")
                            },
                            onAddNewTrack = { title, artist, genre, duration ->
                                viewModel.addNewTrack(title, artist, genre, duration)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        playerViewModel?.resetInactivityTimer()
        return super.onKeyDown(keyCode, event)
    }
}

// Data class to hold metadata extracted from chosen Uri
data class AudioMetadata(val fileName: String, val durationMs: Long, val localPath: String? = null)

// Helper function to extract audio file name and duration safely on a background thread
suspend fun getAudioFileMetadata(context: Context, uri: Uri): AudioMetadata = withContext(Dispatchers.IO) {
    var name = "Imported Track"
    var duration = 180000L // default 3 minutes
    var localPath: String? = null

    // Retrieve file name
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (e: Exception) {
        name = uri.lastPathSegment ?: "Imported Track"
    }

    // Copy file to internal storage for reliable offline persistent playback
    try {
        val cleanName = name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val uniqueName = "${System.currentTimeMillis()}_$cleanName"
        val destFile = java.io.File(context.filesDir, uniqueName)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            java.io.FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        localPath = destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Retrieve track duration
    try {
        val retriever = MediaMetadataRetriever()
        if (localPath != null) {
            retriever.setDataSource(localPath)
        } else {
            retriever.setDataSource(context, uri)
        }
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (durationStr != null) {
            duration = durationStr.toLong()
        }
        retriever.release()
    } catch (e: Exception) {
        // Fallback if media metadata retriever fails on emulator files
    }

    AudioMetadata(fileName = name, durationMs = duration, localPath = localPath)
}
