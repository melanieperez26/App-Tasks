package com.example.unitrack20.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🌙 MODO OSCURO (estilo login)
private val DarkColors = darkColorScheme(
    primary = ButtonBlue,
    onPrimary = AccentWhite,

    background = Color(0xFF061E34),
    onBackground = AccentWhite,

    surface = Color(0xFF0B2740),
    onSurface = AccentWhite,

    secondary = TealLight,
    onSecondary = Color.Black
)

private val LightColors = lightColorScheme(
    primary = ButtonBlue,
    onPrimary = AccentWhite,

    background = AccentLightGray,
    onBackground = Color.Black,

    surface = Color.White,
    onSurface = Color.Black,

    secondary = Teal,
    onSecondary = Color.Black
)


// Estado del tema
data class ThemeState(
    val darkTheme: Boolean? = null,   // null = seguir sistema
    val dynamicColor: Boolean = false // 🔴 APAGADO (muy importante)
)

@Composable
fun UniTrackTheme(
    themeState: ThemeState = ThemeState(),
    content: @Composable () -> Unit
) {
    val useDark = themeState.darkTheme ?: isSystemInDarkTheme()

    val colors = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
