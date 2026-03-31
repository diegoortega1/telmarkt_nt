package com.muxunav.telmarktandroid.presentation.ui.color

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class MuxuColorScheme(
    val background: Color,
    val header: Color,
    val muted: Color,
    val button: Color,
    val onButton: Color,
    val container: Color,
    val border: Color,
    val success: Color,
)

val DarkMuxuColors = MuxuColorScheme(
    background = Color(0xFF151921),
    header = Color(0xFFE6F1FB).copy(alpha = 0.7f),
    muted = Color(0xFFE6F1FB).copy(alpha = 0.5f),
    button = Color(0xFFE24B4A),
    onButton = Color.White,
    container = Color.White.copy(alpha = 0.05f),
    border = Color.White.copy(alpha = 0.1f),
    success = Color(0xFF0F6E56)
)

val LightMuxuColors = MuxuColorScheme(
    background = Color.White,
    header = Color.Black,
    muted = Color(0xFF888780),
    button = Color(0xFFC9302C),
    onButton = Color.White,
    container = Color.Black.copy(alpha = 0.04f),
    border = Color.Black.copy(alpha = 0.1f),
    success = Color(0xFF0F6E56)
)

val LocalMuxuColors = staticCompositionLocalOf { DarkMuxuColors }
