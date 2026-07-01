package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Song
import com.example.ui.LoopMode
import com.example.ui.theme.*

@Composable
fun TVPlayerLayout(
    songsList: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    elapsedTimeMs: Long,
    volume: Int,
    loopMode: LoopMode,
    isShuffle: Boolean,
    onSongSelect: (Song) -> Unit,
    onPlayPauseToggle: () -> Unit,
    onNextSong: () -> Unit,
    onPreviousSong: () -> Unit,
    onAdjustVolume: (Boolean) -> Unit,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit,
    onLoadSamples: () -> Unit,
    onClearPlaylist: () -> Unit,
    onImportLocalFiles: () -> Unit,
    onAddNewTrack: (String, String, String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddTrackDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBgDark)
    ) {
        if (songsList.isEmpty() || currentSong == null) {
            // ==================== BEAUTIFUL IMMERSIVE TV EMPTY STATE ====================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Glowing Vinyl Illustration
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    VinylRecord(
                        isPlaying = false,
                        albumArtRes = null,
                        size = 320.dp,
                        modifier = Modifier.padding(24.dp)
                    )
                    
                    // Ambient pulsing halo
                    val infiniteTransition = rememberInfiniteTransition(label = "empty_halo")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "halo_pulse"
                    )
                    Box(
                        modifier = Modifier
                            .size(340.dp * pulseScale)
                            .border(1.5.dp, TvNeonCyan.copy(alpha = 0.2f), CircleShape)
                            .border(4.dp, TvNeonCyan.copy(alpha = 0.05f), CircleShape)
                    )
                }

                // Right Side: Beautiful Frosted Glass Control Center
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(TvBgCard)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .border(1.5.dp, TvNeonCyan.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Title with Neon Accent
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(TvNeonCyan)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "TV MINIMALIST MUSIC PLAYER",
                            color = TvTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Text(
                        text = "一開始播放清單為空",
                        color = TvNeonPurple,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "極簡、太空星空背景與防烙印 OLED 螢幕保護程式。您可以匯入電視本機音訊，或是一鍵載入精心調製的太空合成器曲目開始聆聽！",
                        color = TvTextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // 3 TV focusable horizontal buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Button 1: Import local audio
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TvNeonCyan.copy(alpha = 0.15f))
                                .tvFocusable(
                                    onClick = onImportLocalFiles,
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBorderColor = TvNeonCyan
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Import",
                                tint = TvNeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "匯入本機音樂",
                                color = TvNeonCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Button 2: Load Sample Tracks
                        Row(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TvNeonPurple.copy(alpha = 0.15f))
                                .tvFocusable(
                                    onClick = onLoadSamples,
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBorderColor = TvNeonPurple
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Curated",
                                tint = TvNeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "載入太空曲目",
                                color = TvNeonPurple,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Button 3: Add Manual
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .tvFocusable(
                                    onClick = { showAddTrackDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBorderColor = Color.White
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom",
                                tint = TvTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "手動新增歌曲",
                                color = TvTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // ==================== FULL ACTIVE TV PLAYER LAYOUT ====================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Left Pane: Glassmorphism Soundtrack Playlist (40% width)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.42f)
                        .padding(end = 16.dp)
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TvNeonCyan)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SOUNDTRACKS",
                                color = TvTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }

                        // Size badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(TvNeonCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${songsList.size} Tracks",
                                color = TvNeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Playlist Scrollable List
                    val scrollState = rememberScrollState()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        songsList.forEachIndexed { index, song ->
                            val isCurrent = song.id == currentSong.id
                            var isFocused by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isFocused) TvBgCardFocused else TvBgCard)
                                    .tvFocusable(
                                        onClick = { onSongSelect(song) },
                                        shape = RoundedCornerShape(14.dp),
                                        focusedBorderColor = TvNeonCyan,
                                        unfocusedBorderColor = if (isCurrent) TvNeonPurple.copy(alpha = 0.5f) else Color.Transparent,
                                        onFocused = { isFocused = true }
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Track Number
                                Text(
                                    text = String.format("%02d", index + 1),
                                    color = if (isCurrent) TvNeonCyan else TvTextMuted,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp)
                                )

                                // Track Art / Placeholder Icon
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (song.albumArtRes != null) {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = song.albumArtRes),
                                            contentDescription = "Song cover artwork",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = "Fallback vinyl record",
                                            tint = TvNeonPurple,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Title & Artist
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) TvNeonCyan else TvTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = song.artist,
                                        color = TvTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Genre Tag & Playing indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.width(90.dp)
                                ) {
                                    if (isCurrent && isPlaying) {
                                        MusicFrequencyBars()
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isCurrent) TvNeonCyan.copy(alpha = 0.15f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = song.genre,
                                            color = if (isCurrent) TvNeonCyan else TvTextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Row: Add and Clear action buttons inside Left Pane
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick Import Files Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvNeonCyan.copy(alpha = 0.1f))
                                .tvFocusable(
                                    onClick = onImportLocalFiles,
                                    shape = RoundedCornerShape(10.dp),
                                    focusedBorderColor = TvNeonCyan
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Import Button",
                                tint = TvNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "匯入音樂",
                                color = TvNeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Add Custom Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .tvFocusable(
                                    onClick = { showAddTrackDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    focusedBorderColor = Color.White
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Track Button",
                                tint = TvTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "新增歌曲",
                                color = TvTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Clear Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1AFF3B30))
                                .tvFocusable(
                                    onClick = onClearPlaylist,
                                    shape = RoundedCornerShape(10.dp),
                                    focusedBorderColor = Color(0xFFFF3B30)
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear List Button",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "清空清單",
                                color = Color(0xFFFF3B30),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Pane: High-end Minimalist Player Center (58% width)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.58f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Center Piece: Spinning Vinyl Disk
                    VinylRecord(
                        isPlaying = isPlaying,
                        albumArtRes = currentSong.albumArtRes,
                        size = 270.dp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Dynamic Song Meta Labels
                    Text(
                        text = currentSong.title,
                        color = TvTextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("track_title")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentSong.artist,
                        color = TvNeonPurple,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("track_artist")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(elapsedTimeMs),
                            color = TvTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(44.dp)
                        )

                        // Custom Linear Progress bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            val progressRatio = if (currentSong.durationMs > 0) {
                                elapsedTimeMs.toFloat() / currentSong.durationMs
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressRatio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(TvNeonPurple, TvNeonCyan)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = formatTime(currentSong.durationMs),
                            color = TvTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Navigation Row: Controls (D-pad navigable)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        // Shuffle Button
                        ControlIconButton(
                            icon = Icons.Default.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            isActive = isShuffle,
                            onClick = onToggleShuffle,
                            modifier = Modifier.testTag("control_shuffle")
                        )

                        // Previous Button
                        ControlIconButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            onClick = onPreviousSong,
                            modifier = Modifier.testTag("control_prev")
                        )

                        // Play / Pause (Extra large floating focal button)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(TvNeonCyan)
                                .tvFocusable(
                                    onClick = onPlayPauseToggle,
                                    shape = CircleShape,
                                    focusedBorderColor = Color.White,
                                    scaleAmount = 1.12f
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = TvBgDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Next Button
                        ControlIconButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            onClick = onNextSong,
                            modifier = Modifier.testTag("control_next")
                        )

                        // Repeat Button (visual states for loop mode)
                        val loopIcon = Icons.Default.Refresh
                        val loopColor = when (loopMode) {
                            LoopMode.NONE -> Color.Transparent
                            LoopMode.REPEAT_ALL -> TvNeonCyan
                            LoopMode.REPEAT_ONE -> TvNeonPurple
                        }
                        ControlIconButton(
                            icon = loopIcon,
                            contentDescription = "Toggle Loop Mode",
                            isActive = loopMode != LoopMode.NONE,
                            activeColor = loopColor,
                            onClick = onToggleLoop,
                            badgeText = if (loopMode == LoopMode.REPEAT_ONE) "1" else null,
                            modifier = Modifier.testTag("control_loop")
                        )
                    }

                    // Volume Control Bar (D-pad focusable)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(0.65f)
                    ) {
                        ControlIconButton(
                            icon = Icons.Default.VolumeDown,
                            contentDescription = "Volume Down",
                            onClick = { onAdjustVolume(false) },
                            size = 38.dp,
                            iconSize = 18.dp,
                            modifier = Modifier.testTag("vol_down")
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Custom segmented volume meter
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val segments = 10
                            val activeSegments = (volume / 10).coerceIn(0, segments)
                            for (i in 1..segments) {
                                val active = i <= activeSegments
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (active) TvNeonCyan.copy(alpha = 0.85f)
                                            else Color.White.copy(alpha = 0.1f)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        ControlIconButton(
                            icon = Icons.Default.VolumeUp,
                            contentDescription = "Volume Up",
                            onClick = { onAdjustVolume(true) },
                            size = 38.dp,
                            iconSize = 18.dp,
                            modifier = Modifier.testTag("vol_up")
                        )
                    }
                }
            }
        }

        // ==================== NEON TV DIALOG FOR ADDING MUSIC MANUALLY ====================
        if (showAddTrackDialog) {
            var inputTitle by remember { mutableStateOf("") }
            var inputArtist by remember { mutableStateOf("") }
            var inputGenre by remember { mutableStateOf("Synthwave") }
            var inputDurationSec by remember { mutableStateOf("180") }

            Dialog(onDismissRequest = { showAddTrackDialog = false }) {
                Surface(
                    modifier = Modifier
                        .width(460.dp)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, TvNeonCyan, RoundedCornerShape(20.dp)),
                    color = Color(0xFA0B0E1A)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = "Add Track Icon",
                                tint = TvNeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "手動新增自訂歌曲",
                                color = TvTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Text Field 1: Song Title
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("歌曲名稱 (Song Title)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TvTextPrimary,
                                unfocusedTextColor = TvTextPrimary,
                                focusedContainerColor = TvBgCardFocused,
                                unfocusedContainerColor = TvBgCard,
                                focusedLabelColor = TvNeonCyan,
                                unfocusedLabelColor = TvTextSecondary,
                                focusedIndicatorColor = TvNeonCyan,
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Text Field 2: Artist Name
                        OutlinedTextField(
                            value = inputArtist,
                            onValueChange = { inputArtist = it },
                            label = { Text("演出歌手 (Artist)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TvTextPrimary,
                                unfocusedTextColor = TvTextPrimary,
                                focusedContainerColor = TvBgCardFocused,
                                unfocusedContainerColor = TvBgCard,
                                focusedLabelColor = TvNeonCyan,
                                unfocusedLabelColor = TvTextSecondary,
                                focusedIndicatorColor = TvNeonCyan,
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row for Genre & Duration
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = inputGenre,
                                onValueChange = { inputGenre = it },
                                label = { Text("曲風 (Genre)") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = TvTextPrimary,
                                    unfocusedTextColor = TvTextPrimary,
                                    focusedContainerColor = TvBgCardFocused,
                                    unfocusedContainerColor = TvBgCard,
                                    focusedLabelColor = TvNeonCyan,
                                    unfocusedLabelColor = TvTextSecondary,
                                    focusedIndicatorColor = TvNeonCyan,
                                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f)
                                )
                            )

                            OutlinedTextField(
                                value = inputDurationSec,
                                onValueChange = { inputDurationSec = it.filter { char -> char.isDigit() } },
                                label = { Text("時長 (秒)") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = TvTextPrimary,
                                    unfocusedTextColor = TvTextPrimary,
                                    focusedContainerColor = TvBgCardFocused,
                                    unfocusedContainerColor = TvBgCard,
                                    focusedLabelColor = TvNeonCyan,
                                    unfocusedLabelColor = TvTextSecondary,
                                    focusedIndicatorColor = TvNeonCyan,
                                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Action Buttons: Cancel and Add
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showAddTrackDialog = false },
                                modifier = Modifier.weight(1.2f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("取消", color = TvTextPrimary, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val duration = inputDurationSec.toLongOrNull() ?: 180L
                                    onAddNewTrack(inputTitle, inputArtist, inputGenre, duration)
                                    showAddTrackDialog = false
                                },
                                modifier = Modifier.weight(1.5f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TvNeonCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("儲存歌曲", color = TvBgDark, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== REUSABLE TV HELPERS ====================

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun MusicFrequencyBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "freq_bars")
    
    val height1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .height(24.dp)
            .padding(bottom = 2.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(height1.dp).background(TvNeonCyan, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).height(height2.dp).background(TvNeonCyan, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).height(height3.dp).background(TvNeonCyan, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun ControlIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeColor: Color = TvNeonCyan,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    badgeText: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isFocused) TvBgCardFocused 
                else if (isActive) activeColor.copy(alpha = 0.1f) 
                else Color.Transparent
            )
            .tvFocusable(
                onClick = onClick,
                shape = CircleShape,
                focusedBorderColor = activeColor,
                unfocusedBorderColor = if (isActive) activeColor.copy(alpha = 0.4f) else Color.Transparent,
                onFocused = { isFocused = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.White else if (isActive) activeColor else TvTextSecondary,
            modifier = Modifier.size(iconSize)
        )
        
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(TvNeonPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
