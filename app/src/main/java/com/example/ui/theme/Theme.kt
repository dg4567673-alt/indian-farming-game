package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VillageLightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = FreshSproutGreen,
    onPrimaryContainer = Color(0xFF07261A),
    secondary = TerracottaWarm,
    onSecondary = Color.White,
    secondaryContainer = HarvestGold,
    onSecondaryContainer = RichSoil,
    tertiary = TractorCrimson,
    onTertiary = Color.White,
    background = CreamEarth,
    onBackground = RichSoil,
    surface = CreamEarth,
    onSurface = RichSoil,
    surfaceVariant = WarmSand,
    onSurfaceVariant = Color(0xFF534538),
    outline = Color(0xFFBBA992)
)

private val VillageDarkColorScheme = darkColorScheme(
    primary = FreshSproutGreen,
    onPrimary = Color(0xFF07261A),
    primaryContainer = ForestGreen,
    onPrimaryContainer = Color(0xFFD8F3DC),
    secondary = HarvestGold,
    onSecondary = RichSoil,
    secondaryContainer = TerracottaWarm,
    onSecondaryContainer = Color.White,
    tertiary = MustardAmber,
    onTertiary = RichSoil,
    background = DarkBark,
    onBackground = CreamEarth,
    surface = VillageCardDark,
    onSurface = CreamEarth,
    surfaceVariant = Color(0xFF313B33),
    onSurfaceVariant = Color(0xFFD3C7B8),
    outline = Color(0xFF6B776D)
)

val VillageShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ShubhamFarmingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VillageDarkColorScheme else VillageLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = VillageShapes,
        content = content
    )
}
