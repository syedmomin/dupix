package dev.bit.dupix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BlueLight,
    secondary = BlueDark,
    tertiary = Amber,
    background = Surface,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = OnSurface,
    onSurface = OnSurface,
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    secondary = Blue,
    tertiary = Amber,
)

@Composable
fun DupixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
