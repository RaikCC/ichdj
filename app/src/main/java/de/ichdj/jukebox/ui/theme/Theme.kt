package de.ichdj.jukebox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Designfarben (siehe improvements.md)
val NeonIce = Color(0xFF2DE2E6) // spielender Track (Rahmen, "jetzt"), Verbunden-Status
val ElectricRose = Color(0xFFF6019D) // gefüllte Wünsche (Rahmen, Nummern)
val Indigo = Color(0xFF650D89) // Steuerelemente (vormals Orange)
val DarkAmethyst = Color(0xFF261447) // reserviert
val MidnightViolet = Color(0xFF0D0221) // reserviert

/** Dezenter Rahmen für neutrale/leere Boxen und Queue-Einträge. */
val NeutralOutline = Color(0x33FFFFFF)

private val DarkColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = NeonIce,
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
