package com.example.uchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = LightBlue40,
    onPrimary        = Color.White,
    primaryContainer = LightBlue90,
    onPrimaryContainer = Blue10,

    secondary        = Blue40,
    onSecondary      = Color.White,
    secondaryContainer = Blue90,
    onSecondaryContainer = Blue10,

    background       = Color.White,
    onBackground     = Neutral10,

    surface          = Color.White,
    onSurface        = Neutral10,
    surfaceVariant   = LightBlue90,
    onSurfaceVariant = Blue20,

    error            = Error40,
    onError          = Color.White,
    errorContainer   = Error90,
    onErrorContainer = Color(0xFF410002),

    outline          = Color(0xFF72787E),
)

@Composable
fun UChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
