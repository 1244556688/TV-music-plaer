package com.example.ui

import android.app.Application
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicDatabase
import com.example.data.Song
import com.example.data.SongRepository
import com.example.data.SampleSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

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
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null

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
        updatePlaybackState()
        if (_isPlaying.value) {
            startPlaybackTimer()
        }
    }

    fun togglePlayPause() {
        resetInactivityTimer()
        if (_currentSong.value == null) return
        val nextPlaying = !_isPlaying.value
        _isPlaying.value = nextPlaying
        updatePlaybackState()
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
        
        val nextSongObj = if (_isShuffle.value) {
            var nextIndex = random.nextInt(songs.size)
            if (songs.size > 1 && nextIndex == currentIndex) {
                nextIndex = (nextIndex + 1) % songs.size
            }
            songs[nextIndex]
        } else {
            val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % songs.size
            songs[nextIndex]
        }
        
        _currentSong.value = nextSongObj
        _elapsedTimeMs.value = 0L
        updatePlaybackState()
        if (_isPlaying.value) {
            startPlaybackTimer()
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
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.seekTo(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val prevIndex = if (currentIndex <= 0) songs.size - 1 else currentIndex - 1
            val prevSongObj = songs[prevIndex]
            _currentSong.value = prevSongObj
            _elapsedTimeMs.value = 0L
            updatePlaybackState()
        }
        
        if (_isPlaying.value) {
            startPlaybackTimer()
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
        updateMediaPlayerVolume()
    }

    fun setVolume(newVolume: Int) {
        resetInactivityTimer()
        _volume.value = newVolume.coerceIn(0, 100)
        updateMediaPlayerVolume()
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
            stopSynth()
            stopMediaPlayer()
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
                
                val mp = mediaPlayer
                val isMpPlaying = try { mp?.isPlaying == true } catch (e: Exception) { false }
                
                if (isMpPlaying && mp != null) {
                    _elapsedTimeMs.value = mp.currentPosition.toLong()
                } else {
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
    }

    private fun handleSongCompletion() {
        val songs = songsList.value
        if (songs.isEmpty()) return
        val current = _currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }

        when (_loopMode.value) {
            LoopMode.REPEAT_ONE -> {
                _elapsedTimeMs.value = 0L
                updatePlaybackState()
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
                    stopSynth()
                    stopMediaPlayer()
                }
            }
        }
    }

    // Audio Playback Engine Coordinators
    private fun updatePlaybackState() {
        val song = _currentSong.value
        val playing = _isPlaying.value
        
        if (song == null || !playing) {
            pauseAudio()
        } else {
            playAudio(song)
        }
    }

    private fun playAudio(song: Song) {
        val fileUri = song.fileUri
        if (!fileUri.isNullOrEmpty()) {
            stopSynth()
            playUri(fileUri)
        } else {
            stopMediaPlayer()
            startSynth(song)
        }
    }

    private fun pauseAudio() {
        stopSynth()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playUri(uriString: String) {
        try {
            stopMediaPlayer()
            val mp = MediaPlayer()
            mediaPlayer = mp
            
            if (uriString.startsWith("content://") || uriString.startsWith("android.resource://")) {
                mp.setDataSource(getApplication(), Uri.parse(uriString))
            } else {
                mp.setDataSource(uriString)
            }
            
            mp.setOnPreparedListener { prepareMp ->
                val vol = _volume.value / 100f
                prepareMp.setVolume(vol, vol)
                if (_isPlaying.value) {
                    prepareMp.start()
                    if (_elapsedTimeMs.value > 0 && _elapsedTimeMs.value < prepareMp.duration) {
                        prepareMp.seekTo(_elapsedTimeMs.value.toInt())
                    }
                }
            }
            
            mp.setOnCompletionListener {
                handleSongCompletion()
            }
            
            mp.setOnErrorListener { _, _, _ ->
                val song = _currentSong.value
                if (song != null) {
                    startSynth(song)
                }
                true
            }
            
            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            val song = _currentSong.value
            if (song != null) {
                startSynth(song)
            }
        }
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    private fun updateMediaPlayerVolume() {
        try {
            mediaPlayer?.let {
                val vol = _volume.value / 100f
                it.setVolume(vol, vol)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSynth(song: Song) {
        stopSynth()
        
        val frequencies = when (song.id) {
            "1" -> doubleArrayOf(130.81, 196.00, 329.63, 493.88)
            "2" -> doubleArrayOf(110.00, 164.81, 261.63, 392.00)
            "3" -> doubleArrayOf(87.31, 130.81, 220.00, 349.23)
            "4" -> doubleArrayOf(98.00, 146.83, 246.94, 392.00)
            "5" -> doubleArrayOf(116.54, 174.61, 293.66, 440.00)
            else -> {
                val hash = song.title.hashCode().coerceAtLeast(0)
                val root = 110.0 + (hash % 110)
                doubleArrayOf(root, root * 1.5, root * 2.0, root * 2.5)
            }
        }

        val sampleRate = 22050
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val actualBufferSize = maxOf(minBufferSize, bufferSize * 2)
        
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(actualBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack = track
            track.play()
            
            synthJob = viewModelScope.launch(Dispatchers.Default) {
                var phase = 0.0
                val volFactor = 0.15f
                
                while (isActive) {
                    val currentVol = _volume.value / 100f
                    val targetAmp = 32767.0 * volFactor * currentVol
                    
                    for (i in 0 until bufferSize) {
                        val t = phase / sampleRate
                        val lfo = 0.6 + 0.4 * sin(2.0 * Math.PI * 0.15 * t)
                        var sum = 0.0
                        for (f in frequencies) {
                            sum += sin(2.0 * Math.PI * f * t)
                        }
                        val sample = (sum / frequencies.size) * targetAmp * lfo
                        buffer[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
                        phase += 1.0
                    }
                    if (isActive) {
                        track.write(buffer, 0, bufferSize)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSynth() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                    track.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        inactivityJob?.cancel()
        stopSynth()
        stopMediaPlayer()
    }
}
