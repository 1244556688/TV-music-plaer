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

    private val _isTvReceiverMode = MutableStateFlow(false)
    val isTvReceiverMode = _isTvReceiverMode.asStateFlow()

    private val _tvReceiverIp = MutableStateFlow("")
    val tvReceiverIp = _tvReceiverIp.asStateFlow()

    private val _isCastingActive = MutableStateFlow(false)
    val isCastingActive = _isCastingActive.asStateFlow()

    private val _targetTvIp = MutableStateFlow("")
    val targetTvIp = _targetTvIp.asStateFlow()

    private val _castStatusMessage = MutableStateFlow("未連接 (Disconnected)")
    val castStatusMessage = _castStatusMessage.asStateFlow()

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
        if (_isCastingActive.value) {
            castPlaySong(song)
            if (!_isPlaying.value) {
                castSendCommand("SET_PLAYING|false")
            }
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
        if (_isCastingActive.value) {
            castSendCommand("SET_PLAYING|$nextPlaying")
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
        if (_isCastingActive.value) {
            castSendCommand("SET_VOLUME|${_volume.value}")
        }
    }

    fun setVolume(newVolume: Int) {
        resetInactivityTimer()
        _volume.value = newVolume.coerceIn(0, 100)
        updateMediaPlayerVolume()
        if (_isCastingActive.value) {
            castSendCommand("SET_VOLUME|${_volume.value}")
        }
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
            var tickCount = 0
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

                // Sync elapsed time to TV every 2 seconds if casting is active
                if (_isCastingActive.value) {
                    tickCount++
                    if (tickCount % 2 == 0) {
                        castSendCommand("SET_ELAPSED|${_elapsedTimeMs.value}")
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
        if (_isCastingActive.value) {
            // When casting is active, the tablet itself should not play/emit sound (remain silent).
            // Stop any active local audio generators (synth or media player).
            stopSynth()
            stopMediaPlayer()
            // Send play command to TV
            castPlaySong(song)
            return
        }

        // Play locally if not casting
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

        // Sync pause to TV if casting is active
        if (_isCastingActive.value) {
            castSendCommand("SET_PLAYING|false")
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

    // ==================== LOCAL CAST NETWORK ENGINE ====================
    private var tvCommandServer: java.net.ServerSocket? = null
    private var tvCommandServerJob: Job? = null
    
    private var castSocket: java.net.Socket? = null
    private var castWriter: java.io.PrintWriter? = null
    private var castJob: Job? = null

    private var audioHttpServer: java.net.ServerSocket? = null
    private var audioHttpServerJob: Job? = null

    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun startTvReceiver() {
        val ip = getLocalIpAddress()
        _tvReceiverIp.value = ip
        _isTvReceiverMode.value = true
        
        tvCommandServerJob?.cancel()
        tvCommandServerJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try listening on port 8888
                val server = java.net.ServerSocket(8888)
                tvCommandServer = server
                while (isActive) {
                    val socket = server.accept()
                    launch(Dispatchers.IO) {
                        try {
                            val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream()))
                            while (isActive) {
                                val line = reader.readLine() ?: break
                                handleRemoteCommand(line)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            try { socket.close() } catch (ex: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopTvReceiver() {
        _isTvReceiverMode.value = false
        tvCommandServerJob?.cancel()
        tvCommandServerJob = null
        try {
            tvCommandServer?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tvCommandServer = null
    }

    private fun handleRemoteCommand(command: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val parts = command.split("|")
                if (parts.isEmpty()) return@launch
                
                when (parts[0]) {
                    "PLAY_SONG" -> {
                        if (parts.size >= 5) {
                            val title = parts[1]
                            val artist = parts[2]
                            val durationMs = parts[3].toLongOrNull() ?: 180000L
                            val fileUri = parts[4]
                            
                            val existingSong = songsList.value.find {
                                it.title.equals(title, ignoreCase = true) &&
                                it.artist.equals(artist, ignoreCase = true)
                            }
                            
                            if (existingSong != null) {
                                _currentSong.value = existingSong
                                _elapsedTimeMs.value = 0L
                                _isPlaying.value = true
                                _isScreensaverActive.value = true // Automatically open screensaver when casting begins
                                updatePlaybackState()
                                startPlaybackTimer()
                            } else {
                                val rxSong = Song(
                                    id = "remote_" + System.currentTimeMillis() + "_" + random.nextInt(100),
                                    title = title,
                                    artist = artist,
                                    durationMs = durationMs,
                                    albumArtRes = null,
                                    genre = "TV Cast",
                                    fileUri = fileUri.ifBlank { null }
                                )
                                // Insert into database so it gets added to the TV's local playlist
                                repository.insertSong(rxSong)
                                
                                _currentSong.value = rxSong
                                _elapsedTimeMs.value = 0L
                                _isPlaying.value = true
                                _isScreensaverActive.value = true // Automatically open screensaver when casting begins
                                updatePlaybackState()
                                startPlaybackTimer()
                            }
                        }
                    }
                    "OPEN_SCREENSAVER" -> {
                        _isScreensaverActive.value = true
                    }
                    "SET_PLAYING" -> {
                        if (parts.size >= 2) {
                            val targetPlaying = parts[1].toBoolean()
                            if (_isPlaying.value != targetPlaying) {
                                _isPlaying.value = targetPlaying
                                updatePlaybackState()
                                if (targetPlaying) {
                                    startPlaybackTimer()
                                } else {
                                    playbackJob?.cancel()
                                }
                            }
                        }
                    }
                    "TOGGLE_PLAY" -> {
                        togglePlayPause()
                    }
                    "NEXT" -> {
                        nextSong()
                    }
                    "PREV" -> {
                        previousSong()
                    }
                    "SET_VOLUME" -> {
                        if (parts.size >= 2) {
                            val vol = parts[1].toIntOrNull() ?: 75
                            setVolume(vol)
                        }
                    }
                    "SET_ELAPSED" -> {
                        if (parts.size >= 2) {
                            val elapsed = parts[1].toLongOrNull() ?: 0L
                            _elapsedTimeMs.value = elapsed
                            mediaPlayer?.let {
                                if (elapsed < it.duration) {
                                    it.seekTo(elapsed.toInt())
                                }
                            }
                        }
                    }
                    "CLEAR_PLAYLIST" -> {
                        clearPlaylist()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun connectToTv(ip: String) {
        _targetTvIp.value = ip
        _castStatusMessage.value = "正在連接至 $ip..."
        
        castJob?.cancel()
        castJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(ip, 8888), 6000)
                castSocket = socket
                castWriter = java.io.PrintWriter(java.io.BufferedWriter(java.io.OutputStreamWriter(socket.getOutputStream())), true)
                
                _isCastingActive.value = true
                _castStatusMessage.value = "已成功連接至電視 ($ip)"
                
                // Start local stream server so TV can stream our local audio files
                startAudioHttpServer()
                
                // Stop local physical audio playing on the tablet immediately, but keep UI playing state active
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    stopSynth()
                    stopMediaPlayer()
                }
                
                // Tell the TV to automatically activate screensaver mode
                castSendCommand("OPEN_SCREENSAVER")
                
                // Immediately stream current song if playing
                val current = _currentSong.value
                if (current != null) {
                    castPlaySong(current)
                    if (!_isPlaying.value) {
                        castSendCommand("SET_PLAYING|false")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isCastingActive.value = false
                _castStatusMessage.value = "連接失敗: ${e.localizedMessage ?: "請檢查 IP 是否正確"}"
            }
        }
    }

    fun disconnectCast() {
        _isCastingActive.value = false
        _castStatusMessage.value = "已中斷連接"
        stopAudioHttpServer()
        castJob?.cancel()
        castJob = null
        try {
            castWriter?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        castWriter = null
        try {
            castSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        castSocket = null

        // Resume local audio playback if we were playing
        if (_isPlaying.value) {
            val current = _currentSong.value
            if (current != null) {
                playAudio(current)
            }
        }
    }

    private fun startAudioHttpServer() {
        stopAudioHttpServer()
        audioHttpServerJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val server = java.net.ServerSocket(8889)
                audioHttpServer = server
                while (isActive) {
                    val socket = server.accept()
                    launch(Dispatchers.IO) {
                        try {
                            val input = socket.getInputStream()
                            val reader = java.io.BufferedReader(java.io.InputStreamReader(input))
                            val requestLine = reader.readLine() ?: ""
                            
                            // Read and consume all headers to prevent TCP RST on socket close
                            var headerLine: String?
                            while (true) {
                                headerLine = reader.readLine()
                                if (headerLine.isNullOrEmpty()) break
                            }
                            
                            if (requestLine.startsWith("GET")) {
                                val currentSongVal = _currentSong.value
                                val localUri = currentSongVal?.fileUri
                                
                                if (localUri != null) {
                                    val output = socket.getOutputStream()
                                    val file = java.io.File(localUri)
                                    if (file.exists()) {
                                        output.write("HTTP/1.1 200 OK\r\n".toByteArray())
                                        output.write("Content-Type: audio/mpeg\r\n".toByteArray())
                                        output.write("Content-Length: ${file.length()}\r\n".toByteArray())
                                        output.write("Accept-Ranges: bytes\r\n".toByteArray())
                                        output.write("Connection: close\r\n\r\n".toByteArray())
                                        
                                        file.inputStream().use { fileInput ->
                                            fileInput.copyTo(output)
                                        }
                                    } else {
                                        try {
                                            val context = getApplication<Application>()
                                            val uri = Uri.parse(localUri)
                                            context.contentResolver.openInputStream(uri)?.use { contentInput ->
                                                output.write("HTTP/1.1 200 OK\r\n".toByteArray())
                                                output.write("Content-Type: audio/mpeg\r\n".toByteArray())
                                                output.write("Accept-Ranges: bytes\r\n".toByteArray())
                                                output.write("Connection: close\r\n\r\n".toByteArray())
                                                contentInput.copyTo(output)
                                            }
                                        } catch (ex: Exception) {
                                            output.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
                                        }
                                    }
                                    output.flush()
                                } else {
                                    val output = socket.getOutputStream()
                                    output.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
                                    output.flush()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            try { socket.close() } catch (ex: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAudioHttpServer() {
        audioHttpServerJob?.cancel()
        audioHttpServerJob = null
        try {
            audioHttpServer?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioHttpServer = null
    }

    private fun castSendCommand(command: String) {
        val writer = castWriter
        if (writer != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    writer.println(command)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun castPlaySong(song: Song) {
        if (!_isCastingActive.value) return
        val phoneIp = getLocalIpAddress()
        val streamUri = if (!song.fileUri.isNullOrEmpty()) {
            "http://$phoneIp:8889/stream"
        } else {
            ""
        }
        val cmd = "PLAY_SONG|${song.title}|${song.artist}|${song.durationMs}|$streamUri"
        castSendCommand(cmd)
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        inactivityJob?.cancel()
        stopSynth()
        stopMediaPlayer()
        stopTvReceiver()
        disconnectCast()
        stopAudioHttpServer()
    }
}
