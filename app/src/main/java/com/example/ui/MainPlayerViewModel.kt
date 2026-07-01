package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicDatabase
import com.example.data.Song
import com.example.data.SongRepository
import com.example.data.SampleSongs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

enum class LoopMode {
    NONE, REPEAT_ONE, REPEAT_ALL
}

class MainPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = MusicDatabase.getDatabase(application).songDao()
    private val repository = SongRepository(songDao)
    private val random = Random()

    // Observe Room DB songs reactively
    val songsList = repository.allSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs = _elapsedTimeMs.asStateFlow()

    private val _volume = MutableStateFlow(75) // Volume from 0 to 100
    val volume = _volume.asStateFlow()

    private val _loopMode = MutableStateFlow(LoopMode.NONE)
    val loopMode = _loopMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _isScreensaverActive = MutableStateFlow(false)
    val isScreensaverActive = _isScreensaverActive.asStateFlow()

    private val _lastActivityTime = MutableStateFlow(System.currentTimeMillis())
    val lastActivityTime = _lastActivityTime.asStateFlow()

    private var playbackJob: Job? = null
    private var inactivityJob: Job? = null

    init {
        startInactivityTracker()
        
        // Auto-select first song when list populated if nothing selected
        viewModelScope.launch {
            songsList.collect { songs ->
                if (songs.isNotEmpty() && _currentSong.value == null) {
                    _currentSong.value = songs.first()
                } else if (songs.isEmpty()) {
                    _currentSong.value = null
                    _isPlaying.value = false
                    playbackJob?.cancel()
                }
            }
        }
    }

    fun resetInactivityTimer() {
        _lastActivityTime.value = System.currentTimeMillis()
        if (_isScreensaverActive.value) {
            _isScreensaverActive.value = false
        }
    }

    private fun startInactivityTracker() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val elapsedSinceLastActivity = System.currentTimeMillis() - _lastActivityTime.value
                if (elapsedSinceLastActivity >= 30000) { // 30 seconds inactivity
                    _isScreensaverActive.value = true
                }
            }
        }
    }

    fun selectSong(song: Song) {
        resetInactivityTimer()
        _currentSong.value = song
        _elapsedTimeMs.value = 0L
        if (_isPlaying.value) {
            startPlaybackTimer()
        }
    }

    fun togglePlayPause() {
        resetInactivityTimer()
        if (_currentSong.value == null) return
        val nextPlaying = !_isPlaying.value
        _isPlaying.value = nextPlaying
        if (nextPlaying) {
            startPlaybackTimer()
        } else {
            playbackJob?.cancel()
        }
    }

    fun nextSong() {
        resetInactivityTimer()
        val songs = songsList.value
        if (songs.isEmpty()) return
        val current = _currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }
        
        if (_isShuffle.value) {
            var nextIndex = random.nextInt(songs.size)
            if (songs.size > 1 && nextIndex == currentIndex) {
                nextIndex = (nextIndex + 1) % songs.size
            }
            selectSong(songs[nextIndex])
        } else {
            val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % songs.size
            selectSong(songs[nextIndex])
        }
    }

    fun previousSong() {
        resetInactivityTimer()
        val songs = songsList.value
        if (songs.isEmpty()) return
        val current = _currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }

        if (_elapsedTimeMs.value > 5000) {
            _elapsedTimeMs.value = 0L
        } else {
            val prevIndex = if (currentIndex <= 0) songs.size - 1 else currentIndex - 1
            selectSong(songs[prevIndex])
        }
    }

    fun adjustVolume(increment: Boolean) {
        resetInactivityTimer()
        val currentVolume = _volume.value
        if (increment) {
            _volume.value = (currentVolume + 5).coerceAtMost(100)
        } else {
            _volume.value = (currentVolume - 5).coerceAtLeast(0)
        }
    }

    fun setVolume(newVolume: Int) {
        resetInactivityTimer()
        _volume.value = newVolume.coerceIn(0, 100)
    }

    fun toggleLoopMode() {
        resetInactivityTimer()
        _loopMode.value = when (_loopMode.value) {
            LoopMode.NONE -> LoopMode.REPEAT_ALL
            LoopMode.REPEAT_ALL -> LoopMode.REPEAT_ONE
            LoopMode.REPEAT_ONE -> LoopMode.NONE
        }
    }

    fun toggleShuffle() {
        resetInactivityTimer()
        _isShuffle.value = !_isShuffle.value
    }

    // Interactive user features
    fun loadSampleSongs() {
        resetInactivityTimer()
        viewModelScope.launch {
            repository.insertSongs(SampleSongs)
        }
    }

    fun clearPlaylist() {
        resetInactivityTimer()
        viewModelScope.launch {
            repository.clearPlaylist()
            _currentSong.value = null
            _isPlaying.value = false
            _elapsedTimeMs.value = 0L
            playbackJob?.cancel()
        }
    }

    fun addNewTrack(title: String, artist: String, genre: String, durationSec: Long) {
        resetInactivityTimer()
        val newSong = Song(
            id = System.currentTimeMillis().toString(),
            title = title.ifBlank { "Untitled Track" },
            artist = artist.ifBlank { "Unknown Artist" },
            durationMs = durationSec * 1000,
            albumArtRes = null, // uses vinyl placeholder
            genre = genre.ifBlank { "Ambient" }
        )
        viewModelScope.launch {
            repository.insertSong(newSong)
        }
    }

    fun importAudioFile(uriString: String, fileName: String, durationMs: Long) {
        resetInactivityTimer()
        // Extract a pretty title/artist from the file name
        val cleanName = fileName.substringBeforeLast(".")
        val parts = cleanName.split("-", "—")
        val title = if (parts.size > 1) parts[1].trim() else cleanName.trim()
        val artist = if (parts.size > 1) parts[0].trim() else "Device Audio"

        val importedSong = Song(
            id = System.currentTimeMillis().toString() + "_" + random.nextInt(1000),
            title = title,
            artist = artist,
            durationMs = if (durationMs > 0) durationMs else 180000L, // 3 mins fallback
            albumArtRes = null,
            genre = "Local Store",
            fileUri = uriString
        )
        viewModelScope.launch {
            repository.insertSong(importedSong)
        }
    }

    private fun startPlaybackTimer() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val currentSongVal = _currentSong.value ?: break
                val currentElapsed = _elapsedTimeMs.value
                val totalDuration = currentSongVal.durationMs

                if (currentElapsed < totalDuration) {
                    _elapsedTimeMs.value = currentElapsed + 1000
                } else {
                    handleSongCompletion()
                    break
                }
            }
        }
    }

    private fun handleSongCompletion() {
        val songs = songsList.value
        if (songs.isEmpty()) return
        val current = _currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }

        when (_loopMode.value) {
            LoopMode.REPEAT_ONE -> {
                _elapsedTimeMs.value = 0L
                startPlaybackTimer()
            }
            LoopMode.REPEAT_ALL -> {
                nextSong()
            }
            LoopMode.NONE -> {
                if (currentIndex < songs.size - 1) {
                    nextSong()
                } else {
                    _isPlaying.value = false
                    _elapsedTimeMs.value = 0L
                    playbackJob?.cancel()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        inactivityJob?.cancel()
    }
}
