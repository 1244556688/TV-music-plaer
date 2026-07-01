package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.TvBgDark
import kotlinx.coroutines.isActive
import java.util.Random

class StarState(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val speed: Float,
    var phase: Float,
    val maxAlpha: Float
)

class ShootingStarState(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val width: Float,
    var progress: Float,
    val speed: Float
)

@Composable
fun StarsCanvas(modifier: Modifier = Modifier) {
    val random = remember { Random() }
    
    // Create stars
    val stars = remember {
        List(120) {
            StarState(
                relX = random.nextFloat(),
                relY = random.nextFloat(),
                radius = 1.0f + random.nextFloat() * 3.0f,
                speed = 0.015f + random.nextFloat() * 0.04f,
                phase = random.nextFloat() * 2f * Math.PI.toFloat(),
                maxAlpha = 0.3f + random.nextFloat() * 0.7f
            )
        }
    }

    var shootingStar by remember { mutableStateOf<ShootingStarState?>(null) }
    var tickTrigger by remember { mutableStateOf(0) }

    // Simulation loop
    LaunchedEffect(Unit) {
        val clock = kotlinx.coroutines.currentCoroutineContext()
        while (clock.isActive) {
            // Twinkle stars
            for (star in stars) {
                star.phase = (star.phase + star.speed) % (2f * Math.PI.toFloat())
            }

            // Move shooting star
            val activeMeteor = shootingStar
            if (activeMeteor != null) {
                activeMeteor.progress += activeMeteor.speed
                if (activeMeteor.progress >= 1.0f) {
                    shootingStar = null
                }
            } else {
                // Occasional spawn (0.3% chance per tick)
                if (random.nextFloat() < 0.003f) {
                    val angle = Math.toRadians((20.0 + random.nextDouble() * 40.0)) // shoot diagonally
                    val startRelX = random.nextFloat() * 0.5f
                    val startRelY = random.nextFloat() * 0.3f
                    val length = 400f + random.nextFloat() * 400f
                    val startX = startRelX
                    val startY = startRelY
                    val endX = startX + (length * Math.cos(angle)).toFloat() / 1920f
                    val endY = startY + (length * Math.sin(angle)).toFloat() / 1080f
                    
                    shootingStar = ShootingStarState(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        width = 1.5f + random.nextFloat() * 2.0f,
                        progress = 0f,
                        speed = 0.015f + random.nextFloat() * 0.025f
                    )
                }
            }

            tickTrigger++
            kotlinx.coroutines.delay(20) // ~50 FPS
        }
    }

    Box(modifier = modifier.fillMaxSize().background(TvBgDark)) {
        // Immersive cosmic space background generated using AI
        Image(
            painter = painterResource(id = R.drawable.img_cosmic_bg_1782904888885),
            contentDescription = "Cosmic Space Wallpaper",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.8f
        )
        
        // Dark gradient vignette overlay to enhance layout readability & minimalism
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xF2010204)),
                        radius = 1200f
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Use state read to trigger re-draw on each simulation tick
            val _trigger = tickTrigger

            // Draw Twinkling Stars
            for (star in stars) {
                val x = star.relX * canvasWidth
                val y = star.relY * canvasHeight
                val twinkleFactor = 0.2f + 0.8f * kotlin.math.sin(star.phase)
                val alpha = (star.maxAlpha * twinkleFactor).coerceIn(0.1f, 1.0f)

                drawCircle(
                    color = Color(0xFFE5F5FF).copy(alpha = alpha),
                    radius = star.radius,
                    center = Offset(x, y)
                )
            }

            // Draw Shooting Star / Meteor with a beautiful glowing trailing tail
            shootingStar?.let { meteor ->
                val startX = meteor.startX * canvasWidth
                val startY = meteor.startY * canvasHeight
                val endX = meteor.endX * canvasWidth
                val endY = meteor.endY * canvasHeight

                val currentX = startX + (endX - startX) * meteor.progress
                val currentY = startY + (endY - startY) * meteor.progress

                // Tail length
                val tailProgress = (meteor.progress - 0.25f).coerceAtLeast(0f)
                val tailStartX = startX + (endX - startX) * tailProgress
                val tailStartY = startY + (endY - startY) * tailProgress

                // Draw glowing streak line with gradient fading to transparency
                if (meteor.progress > 0.01f) {
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color(0xAA00F0FF), Color.White),
                            start = Offset(tailStartX, tailStartY),
                            end = Offset(currentX, currentY)
                        ),
                        start = Offset(tailStartX, tailStartY),
                        end = Offset(currentX, currentY),
                        strokeWidth = meteor.width
                    )

                    // Draw glowing white meteor head
                    drawCircle(
                        color = Color.White,
                        radius = meteor.width * 1.5f,
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}
