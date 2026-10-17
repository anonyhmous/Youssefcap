package com.driveflow.driver.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.ui.components.LicenseStatusChip
import com.driveflow.driver.ui.components.RestrictedSettingsDialog
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

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleReader: (Boolean) -> Unit,
    onToggleHomeWay: (Boolean) -> Unit,
    onToggleApp: (RideApp, Boolean) -> Unit,
    onNavigateRules: () -> Unit,
    onNavigateAlerts: () -> Unit,
    onNavigateHomeWay: () -> Unit,
    onNavigatePlan: () -> Unit,
    onSimulateAccessibility: () -> Unit = {},
    onSimulateOverlay: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRestrictedDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_home"),
        containerColor = NavyBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_driveflow_logo),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_full_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Hero Status Card: DriveFlow is ON / OFF
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_hero_status"),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isReaderRunning) NavySurfaceElevated else NavySurface
                ),
                shape = RoundedCornerShape(20.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (uiState.isReaderRunning) BrandMint else OutlineNavy
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (uiState.isReaderRunning) BrandMint.copy(alpha = 0.2f) else OutlineNavy
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (uiState.isReaderRunning) BrandMint else TextMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(
                                    if (uiState.isReaderRunning) R.string.home_hero_on else R.string.home_hero_off
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (uiState.isReaderRunning) BrandMint else TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    if (uiState.isReaderRunning) R.string.home_hero_on_sub else R.string.home_hero_off_sub
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isReaderRunning,
                        onCheckedChange = { onToggleReader(it) },
                        modifier = Modifier.testTag("switch_reader_master"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyBackground,
                            checkedTrackColor = BrandMint,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = NavySurfaceVariant
                        )
                    )
                }
            }

            // Quick Info Rows Card (Rules, Alerts, Heading Home, Plan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val minPrice = uiState.policy.minPricePerKm
                    val rulesSummary = if (minPrice > 0.0) {
                        "${minPrice} ${uiState.policy.currency}/${uiState.policy.distanceUnit.name}"
                    } else {
                        stringResource(R.string.home_step3_sub)
                    }
                    HomeActionRow(
                        icon = Icons.Default.Tune,
                        iconTint = BrandGold,
                        title = stringResource(R.string.home_row_rules_title),
                        subtitle = rulesSummary,
                        onClick = onNavigateRules
                    )
                    HomeActionRow(
                        icon = Icons.Default.NotificationsActive,
                        iconTint = BrandMint,
                        title = stringResource(R.string.home_row_alert_title),
                        subtitle = if (uiState.isAlertEnabled) stringResource(R.string.alerts_mode_sound) else stringResource(R.string.action_reset),
                        onClick = onNavigateAlerts
                    )
                    HomeActionRow(
                        icon = Icons.Default.Home,
                        iconTint = BrandGold,
                        title = stringResource(R.string.home_row_homeway_title),
                        subtitle = if (uiState.homeWay.enabled) {
                            uiState.homeWay.label ?: stringResource(R.string.homeway_mode_home)
                        } else {
                            stringResource(R.string.home_hero_off)
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.homeWay.enabled,
                                onCheckedChange = { onToggleHomeWay(it) },
                                modifier = Modifier.testTag("switch_homeway_quick"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NavyBackground,
                                    checkedTrackColor = BrandGold
                                )
                            )
                        },
                        onClick = onNavigateHomeWay
                    )
                    HomeActionRow(
                        icon = Icons.Default.DirectionsCar,
                        iconTint = BrandMint,
                        title = stringResource(R.string.home_row_plan_title),
                        subtitle = stringResource(R.string.subscribe_days_left, uiState.daysLeft),
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LicenseStatusChip(status = uiState.licenseStatus)
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = onNavigatePlan
                    )
                }
            }

            // 3-Step Setup Checklist (if incomplete)
            if (!uiState.isSetupComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_setup_checklist"),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BrandGold.copy(alpha = 0.5f))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_checklist_title, uiState.remainingSteps),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandGold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ChecklistItem(
                            title = stringResource(R.string.home_step1_title),
                            subtitle = stringResource(R.string.home_step1_sub),
                            isCompleted = uiState.isAccessibilityGranted,
                            onClick = {
                                showRestrictedDialog = true
                                onSimulateAccessibility()
                            }
                        )
                        ChecklistItem(
                            title = stringResource(R.string.home_step2_title),
                            subtitle = stringResource(R.string.home_step2_sub),
                            isCompleted = uiState.isOverlayGranted,
                            onClick = {
                                onSimulateOverlay()
                            }
                        )
                        ChecklistItem(
                            title = stringResource(R.string.home_step3_title),
                            subtitle = stringResource(R.string.home_step3_sub),
                            isCompleted = uiState.isRulesConfigured,
                            onClick = onNavigateRules
                        )
                    }
                }
            }

            // Keep the reader running (Battery settings)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBatteryDialog = true }
                    .testTag("row_keep_running"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = NavySurfaceElevated
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_keep_running_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_keep_running_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Supported Apps row with switch per app
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
                        text = stringResource(R.string.home_supported_apps_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RideAppSwitchRow(
                        appName = stringResource(R.string.app_indrive),
                        enabled = uiState.policy.appEnabled[RideApp.INDRIVE] == true,
                        onToggle = { onToggleApp(RideApp.INDRIVE, it) }
                    )
                    RideAppSwitchRow(
                        appName = stringResource(R.string.app_uber),
                        enabled = false,
                        subtitle = "قريباً (غير مفعل)",
                        isBlocked = true,
                        onToggle = { onToggleApp(RideApp.UBER, it) }
                    )
                    RideAppSwitchRow(
                        appName = stringResource(R.string.app_didi),
                        enabled = uiState.policy.appEnabled[RideApp.DIDI] == true,
                        onToggle = { onToggleApp(RideApp.DIDI, it) }
                    )
                }
            }
        }
    }

    if (showRestrictedDialog) {
        RestrictedSettingsDialog(
            onDismiss = { showRestrictedDialog = false },
            onOpenAppSettings = { showRestrictedDialog = false }
        )
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.home_keep_running_title),
                    color = BrandGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.guide_step4_desc),
                    color = TextWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text(stringResource(R.string.action_confirm), color = BrandMint)
                }
            },
            containerColor = NavySurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun HomeActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = NavySurfaceElevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = if (isCompleted) VerdictGreen else OutlineNavy
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = NavyBackground, modifier = Modifier.size(18.dp))
                } else {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TextMuted))
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isCompleted) TextMuted else TextWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RideAppSwitchRow(
    appName: String,
    enabled: Boolean,
    subtitle: String? = null,
    isBlocked: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGold
                )
            }
        }
        Switch(
            checked = if (isBlocked) false else enabled,
            onCheckedChange = { if (!isBlocked) onToggle(it) },
            enabled = !isBlocked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NavyBackground,
                checkedTrackColor = BrandMint,
                disabledCheckedThumbColor = TextMuted,
                disabledUncheckedThumbColor = TextMuted
            )
        )
    }
}
