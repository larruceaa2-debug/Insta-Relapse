package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FollowLensSemanticColors(
    val notFollowingBack: Color,
    val notFollowingBackContainer: Color,
    val mutual: Color,
    val mutualContainer: Color,
    val fan: Color,
    val fanContainer: Color,
    val followersAccent: Color,
    val followersContainer: Color,
    val cardElevated: Color,
    val subtleBorder: Color,
)

private val DarkSemanticColors = FollowLensSemanticColors(
    notFollowingBack = CoralPrimary,
    notFollowingBackContainer = CoralContainer,
    mutual = MintTertiary,
    mutualContainer = MintContainer,
    fan = AmberSecondary,
    fanContainer = AmberContainer,
    followersAccent = SkyAccent,
    followersContainer = SkyContainer,
    cardElevated = SlateCard,
    subtleBorder = SlateBorder,
)

private val LightSemanticColors = FollowLensSemanticColors(
    notFollowingBack = CoralPrimaryLight,
    notFollowingBackContainer = CoralContainerLight,
    mutual = MintTertiaryLight,
    mutualContainer = MintContainerLight,
    fan = AmberSecondaryLight,
    fanContainer = AmberContainerLight,
    followersAccent = SkyAccentLight,
    followersContainer = Color(0xFFE0F2FE),
    cardElevated = SoftMistCard,
    subtleBorder = LightBorder,
)

val LocalFollowLensColors = staticCompositionLocalOf { DarkSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = Color(0xFF280510),
    primaryContainer = CoralContainer,
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = AmberSecondary,
    onSecondary = Color(0xFF2A1600),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFFFFDFC0),
    tertiary = MintTertiary,
    onTertiary = Color(0xFF002919),
    tertiaryContainer = MintContainer,
    onTertiaryContainer = Color(0xFFB7F8DC),
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = CharcoalSurface,
    onSurface = IvoryText,
    surfaceVariant = SlateCard,
    onSurfaceVariant = MutedSlateText,
    outline = SlateBorder,
    outlineVariant = Color(0xFF232638),
    error = ErrorRed,
    onError = Color(0xFF380005),
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = Color(0xFF4B091D),
    secondary = AmberSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = Color(0xFF452300),
    tertiary = MintTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = MintContainerLight,
    onTertiaryContainer = Color(0xFF002114),
    background = AlabasterBg,
    onBackground = InkText,
    surface = CrispWhiteSurface,
    onSurface = InkText,
    surfaceVariant = SoftMistCard,
    onSurfaceVariant = GraphiteText,
    outline = LightBorder,
    outlineVariant = Color(0xFFE8ECF5),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

val FollowLensShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FollowLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalFollowLensColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FollowLensTypography,
            shapes = FollowLensShapes,
            content = content,
        )
    }
}
