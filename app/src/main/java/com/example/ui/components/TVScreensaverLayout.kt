package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Song
import com.example.ui.theme.*

@Composable
fun TVScreensaverLayout(
    currentSong: Song?,
    isPlaying: Boolean,
    elapsedTimeMs: Long,
    modifier: Modifier = Modifier
) {
    // Infinite slow floating translation to protect OLED displays from screen burn-in
    val infiniteTransition = rememberInfiniteTransition(label = "screensaver_floating")
    
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vertical_bobbing"
    )

    val floatOffsetX by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "horizontal_drifting"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Deep blue starry universe canvas with diagonal shooting stars
        StarsCanvas(modifier = Modifier.fillMaxSize())

        // Ambient glow behind the main floating card
        Box(
            modifier = Modifier
                .offset(x = floatOffsetX.dp, y = floatOffsetY.dp)
                .size(width = 540.dp, height = 320.dp)
                .shadow(120.dp, RoundedCornerShape(24.dp), spotColor = TvNeonCyan.copy(alpha = 0.5f), ambientColor = TvNeonCyan.copy(alpha = 0.5f))
        )

        // Floating Centered Frosted Glass Info Card
        Column(
            modifier = Modifier
                .offset(x = floatOffsetX.dp, y = floatOffsetY.dp)
                .width(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xD90A0D18)) // frosted glass look
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .border(1.5.dp, TvNeonCyan.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spinning disc or ambient music note badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TvNeonCyan.copy(alpha = 0.1f))
                    .border(1.dp, TvNeonCyan.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (currentSong?.albumArtRes != null) {
                    // Small circular album art sticker inside the canvas
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = currentSong.albumArtRes),
                        contentDescription = "Song artwork thumbnail",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music Playing indicator",
                        tint = TvNeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Song name and artist
            Text(
                text = currentSong?.title ?: "極簡電視音樂播放器",
                color = TvTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = currentSong?.artist ?: "播放清單無歌曲",
                color = TvNeonPurple,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Playback progress slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(
                    text = formatTime(if (currentSong != null) elapsedTimeMs else 0L),
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(42.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    val duration = currentSong?.durationMs ?: 0L
                    val progressRatio = if (duration > 0) {
                        elapsedTimeMs.toFloat() / duration
                    } else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(TvNeonPurple, TvNeonCyan)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = formatTime(currentSong?.durationMs ?: 0L),
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle state: Playing vs Paused vs Idle
            Text(
                text = if (currentSong == null) "AWAITING TRACKS" else if (isPlaying) "NOW PLAYING" else "PAUSED",
                color = if (isPlaying && currentSong != null) TvNeonCyan.copy(alpha = 0.7f) else TvTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        // Floating instructions at bottom screen edge (also drifts slightly to prevent static burn-in)
        Text(
            text = "Press any remote key to wake player",
            color = TvTextMuted.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = -floatOffsetX.dp * 0.2f, y = -48.dp)
        )
    }
}
