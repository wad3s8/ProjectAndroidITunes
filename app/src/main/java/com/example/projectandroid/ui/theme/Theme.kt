package com.example.projectandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A354F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3A1020),
    secondaryContainer = Color(0xFFEDE5E0),
    onSecondaryContainer = Color(0xFF332C29),
    tertiaryContainer = Color(0xFFFFE2A8),
    onTertiaryContainer = Color(0xFF372B10),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
    surfaceContainer = Color(0xFFF4EBE7),
    surfaceContainerLow = Color(0xFFF8EFEB),
    onSurface = Color(0xFF251E20),
    onSurfaceVariant = Color(0xFF69585D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFAEC5),
    primaryContainer = Color(0xFF68283D),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondaryContainer = Color(0xFF3F3437),
    onSecondaryContainer = Color(0xFFF1E3E6),
    tertiaryContainer = Color(0xFF544321),
    onTertiaryContainer = Color(0xFFFFE2A8),
    background = Color(0xFF191416),
    surface = Color(0xFF191416),
    surfaceContainer = Color(0xFF292124),
    surfaceContainerLow = Color(0xFF241D20),
    onSurface = Color(0xFFF2E5E9),
    onSurfaceVariant = Color(0xFFD4BFC6),
)

@Composable
fun ProjectAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}

