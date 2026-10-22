package com.sueldo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Palette – professional finance feel
val Emerald = Color(0xFF0F766E)
val EmeraldLight = Color(0xFF14B8A6)
val EmeraldDark = Color(0xFF134E4A)
val Amber = Color(0xFFF59E0B)
val AmberSoft = Color(0xFFFEF3C7)
val Rose = Color(0xFFE11D48)
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = EmeraldDark,
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = AmberSoft,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF6366F1),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Color(0xFF64748B),
    error = Rose,
    outline = Color(0xFFCBD5E1)
)

private val DarkColors = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = EmeraldDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Amber,
    onSecondary = Color(0xFF78350F),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = AmberSoft,
    tertiary = Color(0xFFA5B4FC),
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFFB7185),
    outline = Color(0xFF475569)
)

@Composable
fun SueldoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
