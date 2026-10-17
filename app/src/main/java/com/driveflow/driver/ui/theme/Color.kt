package com.driveflow.driver.ui.theme

import androidx.compose.ui.graphics.Color
import com.driveflow.driver.core.model.Tone

val NavyBackground = Color(0xFF07131B)
val NavySurface = Color(0xFF0E2230)
val NavySurfaceVariant = Color(0xFF132B3C)
val NavySurfaceElevated = Color(0xFF18384E)
val BrandGold = Color(0xFFF6C945)
val BrandMint = Color(0xFF7CF5C4)
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF9FB3C2)
val OutlineNavy = Color(0xFF22435A)

val VerdictGreen = Color(0xFF22C55E)
val VerdictOrange = Color(0xFFF59E0B)
val VerdictRed = Color(0xFFEF4444)
val VerdictGray = Color(0xFF64748B)

fun Tone.toColor(): Color = when (this) {
    Tone.GREEN -> VerdictGreen
    Tone.ORANGE -> VerdictOrange
    Tone.RED -> VerdictRed
    Tone.GRAY -> VerdictGray
}
