package com.example.openstream.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF7DD3C0),
    onPrimary = Color(0xFF00382F),
    background = Color(0xFF101417),
    surface = Color(0xFF171C20),
    onSurface = Color(0xFFE2E6E9)
)

private val Light = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    background = Color(0xFFF6FAF9),
    surface = Color.White,
    onSurface = Color(0xFF191C1D)
)

@Composable
fun OpenStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}
