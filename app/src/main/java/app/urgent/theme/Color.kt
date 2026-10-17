package app.urgent.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// GENERATED — do not hand-edit individual roles.
//
// Both schemes are derived from four brand seeds by Material's tonal palette
// construction (CAM16/HCT): the seeds are the judgement, the ~36 roles are
// arithmetic. Hand-picking a role breaks the fixed tonal relationship it holds
// with the rest of its family, and the failure shows up as a contrast bug on one
// screen out of thirty rather than as a visible mistake here.
//
//   primary   #4C6FA5  — brand: filled buttons, FAB, selected tab
//   secondary #6B7A8F  — quiet containers: chips, tonal buttons, selected segment
//   tertiary  #CC8000  — accents and caution markers, used sparingly
//   neutral   #E8DBCF  — seeds every surface, so it decides warm or cool overall
//
// Error stays M3 baseline (hue 25, chroma 84): a safety signal, not a brand choice.
//
// To change the palette, edit the seeds in scripts/gen_scheme.py and re-run it.
// Colours come from MaterialTheme, never literals, and dynamic colour defaults
// off (ADR-0006) so this file is what a user actually sees.

private val AppLightColors =
    lightColorScheme(
        primary = Color(0xFF3B5F94),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD4E3FF),
        onPrimaryContainer = Color(0xFF001B3E),
        inversePrimary = Color(0xFFA6C8FF),
        secondary = Color(0xFF515F73),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD4E3FB),
        onSecondaryContainer = Color(0xFF0D1C2E),
        tertiary = Color(0xFF875200),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDDB7),
        onTertiaryContainer = Color(0xFF2C1700),
        error = Color(0xFFBA1B1B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        background = Color(0xFFFFF8F2),
        onBackground = Color(0xFF211A13),
        surface = Color(0xFFFFF8F2),
        onSurface = Color(0xFF211A13),
        surfaceVariant = Color(0xFFF0E0D0),
        onSurfaceVariant = Color(0xFF504539),
        surfaceDim = Color(0xFFE5D8CC),
        surfaceBright = Color(0xFFFFF8F2),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFF2E6),
        surfaceContainer = Color(0xFFFAECE0),
        surfaceContainerHigh = Color(0xFFF3E6DA),
        surfaceContainerHighest = Color(0xFFEDE0D4),
        inverseSurface = Color(0xFF362F27),
        inverseOnSurface = Color(0xFFFCEEE2),
        outline = Color(0xFF817568),
        outlineVariant = Color(0xFFD4C4B4),
        scrim = Color(0xFF000000),
    )

private val AppDarkColors =
    darkColorScheme(
        primary = Color(0xFFA6C8FF),
        onPrimary = Color(0xFF003062),
        primaryContainer = Color(0xFF21477A),
        onPrimaryContainer = Color(0xFFD4E3FF),
        inversePrimary = Color(0xFF3B5F94),
        secondary = Color(0xFFB8C8DF),
        onSecondary = Color(0xFF223143),
        secondaryContainer = Color(0xFF39485B),
        onSecondaryContainer = Color(0xFFD4E3FB),
        tertiary = Color(0xFFFFB85D),
        onTertiary = Color(0xFF482A00),
        tertiaryContainer = Color(0xFF663D00),
        onTertiaryContainer = Color(0xFFFFDDB7),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
        errorContainer = Color(0xFF930006),
        onErrorContainer = Color(0xFFFFDAD4),
        background = Color(0xFF18120B),
        onBackground = Color(0xFFEDE0D4),
        surface = Color(0xFF18120B),
        onSurface = Color(0xFFEDE0D4),
        surfaceVariant = Color(0xFF504539),
        onSurfaceVariant = Color(0xFFD4C4B4),
        surfaceDim = Color(0xFF18120B),
        surfaceBright = Color(0xFF3F3830),
        surfaceContainerLowest = Color(0xFF120D07),
        surfaceContainerLow = Color(0xFF211A13),
        surfaceContainer = Color(0xFF251F17),
        surfaceContainerHigh = Color(0xFF302921),
        surfaceContainerHighest = Color(0xFF3B342C),
        inverseSurface = Color(0xFFEDE0D4),
        inverseOnSurface = Color(0xFF362F27),
        outline = Color(0xFF9C8E80),
        outlineVariant = Color(0xFF504539),
        scrim = Color(0xFF000000),
    )

internal val LightColors = AppLightColors
internal val DarkColors = AppDarkColors
