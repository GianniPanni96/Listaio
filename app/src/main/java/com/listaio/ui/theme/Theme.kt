package com.listaio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = GreenLight,
    secondary = GreenDark,
    background = Sand,
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = GreenLight,
    primaryContainer = GreenDark,
)

@Composable
fun ListaioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You) on Android 12+.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
