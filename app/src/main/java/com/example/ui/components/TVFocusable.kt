package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TvNeonCyan

@Composable
fun Modifier.tvFocusable(
    onClick: () -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color = TvNeonCyan,
    unfocusedBorderColor: Color = Color.Transparent,
    scaleAmount: Float = 1.05f,
    onFocused: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleAmount else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tv_focus_scale"
    )

    return this
        .onFocusChanged {
            isFocused = it.isFocused
            if (it.isFocused) {
                onFocused()
            }
        }
        .scale(scale)
        .border(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = shape
        )
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // TV interface uses focus borders/scaling instead of standard mobile ripple overlays
            onClick = onClick
        )
        .focusable()
}
