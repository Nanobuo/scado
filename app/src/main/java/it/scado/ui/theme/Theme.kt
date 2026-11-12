package it.scado.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF1B8A6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5F1D8),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4C6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE9DA),
    onSecondaryContainer = Color(0xFF092016),
    tertiary = Color(0xFFC4573A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD1),
    onTertiaryContainer = Color(0xFF3B0900),
    background = Color(0xFFF6FBF7),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6FBF7),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF404944),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5F1),
    surfaceContainer = Color(0xFFEAEFEB),
    surfaceContainerHigh = Color(0xFFE4EAE5),
    surfaceContainerHighest = Color(0xFFDEE4DF),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8AD5B6),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF005139),
    onPrimaryContainer = Color(0xFFA6F2D1),
    secondary = Color(0xFFB3CCBF),
    onSecondary = Color(0xFF1F352B),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCEE9DA),
    tertiary = Color(0xFFFFB59F),
    onTertiary = Color(0xFF5E1603),
    tertiaryContainer = Color(0xFF7D2B15),
    onTertiaryContainer = Color(0xFFFFDBD1),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404944),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

/** Colore che indica quanto manca alla scadenza. */
fun urgencyColor(daysLeft: Long, dark: Boolean): Color = when {
    daysLeft < 0 -> if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
    daysLeft <= 1 -> if (dark) Color(0xFFFFB68A) else Color(0xFFD9480F)
    daysLeft <= 3 -> if (dark) Color(0xFFF5C451) else Color(0xFFB07700)
    daysLeft <= 7 -> if (dark) Color(0xFF9CCBFF) else Color(0xFF1C6BB0)
    else -> if (dark) Color(0xFF8AD5B6) else Color(0xFF1B8A6B)
}

@Composable
fun ScadoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = AppShapes,
        content = content,
    )
}
