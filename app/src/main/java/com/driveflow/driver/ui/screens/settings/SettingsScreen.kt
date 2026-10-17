package com.driveflow.driver.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.provider.Settings as AndroidSettings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.ui.components.DriveFlowTopBar
import com.driveflow.driver.ui.theme.BrandGold
import com.driveflow.driver.ui.theme.BrandMint
import com.driveflow.driver.ui.theme.NavyBackground
import com.driveflow.driver.ui.theme.NavySurface
import com.driveflow.driver.ui.theme.NavySurfaceElevated
import com.driveflow.driver.ui.theme.NavySurfaceVariant
import com.driveflow.driver.ui.theme.OutlineNavy
import com.driveflow.driver.ui.theme.TextMuted
import com.driveflow.driver.ui.theme.TextWhite
import com.driveflow.driver.ui.theme.VerdictGreen
import com.driveflow.driver.ui.theme.toColor

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSetMarkerSize: (MarkerSize) -> Unit,
    onSetBarOpacity: (Int) -> Unit,
    onSetBarPositionAuto: (Boolean) -> Unit,
    onSetAutoOffHours: (Int) -> Unit,
    onToggleLanguage: () -> Unit,
    onNavigateSubscribe: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_settings"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.settings_screen_title)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleLanguage() }
                    .testTag("row_language_switch"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = NavySurfaceElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = BrandMint, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.language_switch),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = if (uiState.language == "ar") "العربية (مصر)" else "English",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandGold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onToggleLanguage,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandMint),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BrandMint)
                        )
                    ) {
                        Text(if (uiState.language == "ar") "English" else "العربية")
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateSubscribe() }
                    .testTag("row_subscribe_quick"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = NavySurfaceElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CardMembership, contentDescription = null, tint = BrandGold, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.subscribe_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "إدارة خطتك وتفعيل الأكواد",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_marker_size_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.markerSize == MarkerSize.SMALL,
                            onClick = { onSetMarkerSize(MarkerSize.SMALL) },
                            label = { Text(stringResource(R.string.settings_size_small)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.markerSize == MarkerSize.NORMAL,
                            onClick = { onSetMarkerSize(MarkerSize.NORMAL) },
                            label = { Text(stringResource(R.string.settings_size_normal)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandGold,
                                selectedLabelColor = NavyBackground
                            )
                        )
                        FilterChip(
                            selected = uiState.markerSize == MarkerSize.LARGE,
                            onClick = { onSetMarkerSize(MarkerSize.LARGE) },
                            label = { Text(stringResource(R.string.settings_size_large)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_preview_sample_requests),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SampleRequestCard(
                            app = "inDrive",
                            fare = "120 ج.م",
                            distance = "9.5 كم",
                            ratePerKm = "12.6 ج/كم",
                            tone = Tone.GREEN,
                            markerSize = uiState.markerSize
                        )
                        SampleRequestCard(
                            app = "Uber",
                            fare = "65 ج.م",
                            distance = "8.2 كم",
                            ratePerKm = "7.9 ج/كم",
                            tone = Tone.ORANGE,
                            markerSize = uiState.markerSize
                        )
                        SampleRequestCard(
                            app = "DiDi",
                            fare = "30 ج.م",
                            distance = "7.0 كم",
                            ratePerKm = "4.2 ج/كم",
                            tone = Tone.RED,
                            markerSize = uiState.markerSize
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_bar_opacity_title, uiState.barOpacityPercent),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = uiState.barOpacityPercent.toFloat(),
                        onValueChange = { onSetBarOpacity(it.toInt()) },
                        valueRange = 20f..100f,
                        modifier = Modifier.fillMaxWidth().testTag("slider_opacity"),
                        colors = SliderDefaults.colors(
                            thumbColor = BrandMint,
                            activeTrackColor = BrandMint,
                            inactiveTrackColor = NavySurfaceElevated
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("معاينة شكل الشريط فوق الخريطة:", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NavySurface.copy(alpha = uiState.barOpacityPercent / 100f),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BrandMint.copy(alpha = 0.6f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(VerdictGreen)
                                )
                                Text(
                                    text = "DriveFlow: 12.5 ج/كم صافي",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "اقتراح: 135 ج.م (+15)",
                                color = BrandMint,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_bar_position_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.barPosition.isAuto,
                            onClick = { onSetBarPositionAuto(true) },
                            label = { Text(stringResource(R.string.settings_bar_pos_auto)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !uiState.barPosition.isAuto,
                            onClick = { onSetBarPositionAuto(false) },
                            label = { Text(stringResource(R.string.settings_bar_pos_custom)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!uiState.barPosition.isAuto) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onSetBarPositionAuto(true) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NavySurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val context = LocalContext.current
                            val diagText = if (AndroidSettings.canDrawOverlays(context)) {
                                stringResource(R.string.settings_overlay_diag_application)
                            } else {
                                stringResource(R.string.settings_overlay_diag_accessibility)
                            }
                            Text(
                                text = "${stringResource(R.string.settings_overlay_diag_title)}: $diagText",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_auto_off_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val autoOffOptions = listOf(
                        1 to stringResource(R.string.settings_auto_off_1h),
                        3 to stringResource(R.string.settings_auto_off_3h),
                        6 to stringResource(R.string.settings_auto_off_6h),
                        0 to stringResource(R.string.settings_auto_off_never)
                    )
                    autoOffOptions.forEach { (hours, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetAutoOffHours(hours) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (uiState.autoOffHours == hours) BrandGold else OutlineNavy, CircleShape)
                                    .padding(3.dp)
                                    .background(if (uiState.autoOffHours == hours) BrandGold else Color.Transparent, CircleShape)
                            )
                            Text(
                                text = label,
                                color = if (uiState.autoOffHours == hours) TextWhite else TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NavySurfaceElevated,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BrandGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = stringResource(R.string.settings_bank_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleRequestCard(
    app: String,
    fare: String,
    distance: String,
    ratePerKm: String,
    tone: Tone,
    markerSize: MarkerSize
) {
    val badgeSize = when (markerSize) {
        MarkerSize.SMALL -> 24.dp
        MarkerSize.NORMAL -> 30.dp
        MarkerSize.LARGE -> 36.dp
    }

    Surface(
        color = NavySurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(badgeSize),
                    shape = RoundedCornerShape(6.dp),
                    color = tone.toColor()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (tone == Tone.GREEN) "✓" else if (tone == Tone.ORANGE) "!" else "✕",
                            color = NavyBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = when (markerSize) {
                                MarkerSize.SMALL -> 11.sp
                                MarkerSize.NORMAL -> 14.sp
                                MarkerSize.LARGE -> 18.sp
                            }
                        )
                    }
                }
                Column {
                    Text(text = app, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "$fare • $distance", color = TextMuted, fontSize = 11.sp)
                }
            }
            Text(
                text = ratePerKm,
                color = tone.toColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
