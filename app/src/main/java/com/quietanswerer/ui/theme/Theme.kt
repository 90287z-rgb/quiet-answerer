package com.quietanswerer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.quietanswerer.core.Prefs

private val LightColors = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775652),
    onSecondary = Color.White,
    background = Color(0xFFFFF8F7),
    surface = Color(0xFFFFF8F7),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    onSurface = Color(0xFF221A19),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE7988F),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF8E1816),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442925),
    background = Color(0xFF1A1211),
    surface = Color(0xFF1A1211),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    onSurface = Color(0xFFF2DEDB),
    error = Color(0xFFFFB4AB)
)

/** Итоговая схема: светлая/тёмная явно или по системной настройке. */
@Composable
fun isDarkFor(mode: Int): Boolean = when (mode) {
    Prefs.THEME_LIGHT -> false
    Prefs.THEME_DARK -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun QuietAnswererTheme(
    mode: Int = Prefs.themeMode(LocalContext.current),
    content: @Composable () -> Unit
) {
    val colors = if (isDarkFor(mode)) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

/** Подбирает цвет иконок системного статубара под выбранную тему, а не под ночной режим системы. */
@Composable
fun ApplyStatusBarStyle(mode: Int) {
    val context = LocalContext.current
    val darkActive = isDarkFor(mode)
    SideEffect {
        val window = (context as? android.app.Activity)?.window ?: return@SideEffect
        runCatching {
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !darkActive
        }
    }
}