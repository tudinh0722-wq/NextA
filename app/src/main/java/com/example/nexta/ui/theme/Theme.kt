package com.example.nexta.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary                = AccentBlue,
    onPrimary              = CardWhite,
    primaryContainer       = AccentBlueLight,
    onPrimaryContainer     = AccentBlue,
    secondary              = AccentBlue,
    onSecondary            = CardWhite,
    background             = IvoryBackground,
    onBackground           = TextPrimary,
    surface                = IvoryBackground,
    onSurface              = TextPrimary,
    surfaceContainer       = CardWhite,
    surfaceContainerLow    = SurfaceVariant,
    surfaceContainerHighest= SurfaceVariant,
    onSurfaceVariant       = TextSecondary,
)

@Composable
fun NextaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
