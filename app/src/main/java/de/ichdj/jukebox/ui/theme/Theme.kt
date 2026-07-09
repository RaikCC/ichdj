package de.ichdj.jukebox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Helle Hinterlegung für Wünsche (Boxen und Queue-Einträge). */
val WishContainer = Color(0xFFFFE3B3)
val OnWishContainer = Color(0xFF3A2600)
val SpotifyGreen = Color(0xFF1DB954)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF201400),
    secondary = SpotifyGreen,
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFCCCCCC),
    error = Color(0xFFFF6B6B),
)

@Composable
fun IchDjTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
