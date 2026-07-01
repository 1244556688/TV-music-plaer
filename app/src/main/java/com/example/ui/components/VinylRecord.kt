package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.TvNeonCyan

@Composable
fun VinylRecord(
    isPlaying: Boolean,
    albumArtRes: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    // Infinite rotation animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_degree"
    )

    // Smoothly transition between rotation and static pause
    var currentRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // Let the rotation flow from the animation
        } else {
            // Keep the last rotation or slowly halt (we keep simple halt for performance)
        }
    }

    val activeRotation = if (isPlaying) rotation else currentRotation

    Box(
        modifier = modifier
            .size(size)
            .shadow(24.dp, CircleShape, spotColor = TvNeonCyan, ambientColor = TvNeonCyan)
            .rotate(activeRotation),
        contentAlignment = Alignment.Center
    ) {
        // Base Vinyl Disc (uses generated high-fidelity asset)
        Image(
            painter = painterResource(id = R.drawable.img_vinyl_placeholder_1782904928756),
            contentDescription = "Vinyl Record Disc",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Center Album Sticker
        val stickerSize = size * 0.45f
        Box(
            modifier = Modifier
                .size(stickerSize)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF111827), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (albumArtRes != null) {
                Image(
                    painter = painterResource(id = albumArtRes),
                    contentDescription = "Album Art Sticker",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback sticker center when albumArtRes is null
                Image(
                    painter = painterResource(id = R.drawable.img_vinyl_placeholder_1782904928756),
                    contentDescription = "Default Sticker",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Central core spindle hole
            Box(
                modifier = Modifier
                    .size(stickerSize * 0.15f)
                    .clip(CircleShape)
                    .border(2.dp, Color.Black, CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
            )
        }

        // Sleek glossy refraction overlay (subtle transparent circles)
        Box(
            modifier = Modifier
                .fillMaxSize(0.95f)
                .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.75f)
                .border(0.5.dp, Color.White.copy(alpha = 0.03f), CircleShape)
        )
    }
}
