package com.driveflow.driver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

private val DarkColorScheme = darkColorScheme(
    primary = BrandGold,
    onPrimary = NavyBackground,
    primaryContainer = NavySurfaceElevated,
    onPrimaryContainer = BrandGold,
    secondary = BrandMint,
    onSecondary = NavyBackground,
    secondaryContainer = NavySurfaceVariant,
    onSecondaryContainer = BrandMint,
    tertiary = BrandMint,
    onTertiary = NavyBackground,
    background = NavyBackground,
    onBackground = TextWhite,
    surface = NavySurface,
    onSurface = TextWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = OutlineNavy,
    outlineVariant = NavySurfaceVariant,
    error = VerdictRed,
    onError = TextWhite
)

@Composable
fun DriveFlowTheme(
    forceRtl: Boolean? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val currentLocale = configuration.locales[0] ?: Locale.getDefault()
    val isRtl = forceRtl ?: (currentLocale.language == "ar")
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
