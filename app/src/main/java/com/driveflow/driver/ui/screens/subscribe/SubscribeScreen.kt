package com.driveflow.driver.ui.screens.subscribe

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.ui.components.DriveFlowTopBar
import com.driveflow.driver.ui.components.LicenseStatusChip
import com.driveflow.driver.ui.theme.BrandGold
import com.driveflow.driver.ui.theme.BrandMint
import com.driveflow.driver.ui.theme.NavyBackground
import com.driveflow.driver.ui.theme.NavySurface
import com.driveflow.driver.ui.theme.NavySurfaceElevated
import com.driveflow.driver.ui.theme.OutlineNavy
import com.driveflow.driver.ui.theme.TextMuted
import com.driveflow.driver.ui.theme.TextWhite
import com.driveflow.driver.ui.theme.VerdictGreen
import com.driveflow.driver.ui.theme.VerdictRed

@Composable
fun SubscribeScreen(
    uiState: SubscribeUiState,
    onSetLicenseKey: (String) -> Unit,
    onSetAffiliateCode: (String) -> Unit,
    onActivate: () -> Unit,
    onBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isDataSentExpanded by remember { mutableStateOf(false) }

    val errorStringRes = when (uiState.errorCode) {
        "INVALID_KEY" -> R.string.err_api_invalid_key
        "KEY_NOT_FOUND" -> R.string.err_api_key_not_found
        "KEY_ON_OTHER_DEVICE" -> R.string.err_api_key_on_other_device
        "REVOKED" -> R.string.err_api_revoked
        "RATE_LIMITED" -> R.string.err_api_rate_limited
        "AFFILIATE_UNKNOWN" -> R.string.err_api_affiliate_unknown
        "AFFILIATE_SELF" -> R.string.err_api_affiliate_self
        "GIFT_EXPIRED" -> R.string.err_api_gift_expired
        "GIFT_FULL" -> R.string.err_api_gift_full
        "GIFT_USED" -> R.string.err_api_gift_used
        "BAD_REQUEST" -> R.string.err_api_bad_request
        else -> null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_subscribe"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.subscribe_title),
                onBack = onBack
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
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.subscribe_current_status, ""),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LicenseStatusChip(status = uiState.licenseStatus)
                        }
                        Text(
                            text = stringResource(R.string.subscribe_days_left, uiState.planDaysLeft),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandGold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.priceText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandMint,
                        fontWeight = FontWeight.SemiBold
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.subscribe_key_label),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    OutlinedTextField(
                        value = uiState.licenseKey,
                        onValueChange = onSetLicenseKey,
                        placeholder = { Text(stringResource(R.string.subscribe_key_hint), color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = BrandGold)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_license_key"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandGold,
                            unfocusedBorderColor = OutlineNavy,
                            focusedContainerColor = NavySurfaceElevated,
                            unfocusedContainerColor = NavySurfaceElevated
                        ),
                        singleLine = true
                    )
                    Text(
                        text = stringResource(R.string.subscribe_affiliate_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = uiState.affiliateCode,
                        onValueChange = onSetAffiliateCode,
                        placeholder = { Text("مثال: CAPTAIN2026", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_affiliate_code"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandMint,
                            unfocusedBorderColor = OutlineNavy,
                            focusedContainerColor = NavySurfaceElevated,
                            unfocusedContainerColor = NavySurfaceElevated
                        ),
                        singleLine = true
                    )
                    if (errorStringRes != null) {
                        Surface(
                            color = VerdictRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(VerdictRed)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = VerdictRed, modifier = Modifier.size(18.dp))
                                Text(
                                    text = stringResource(id = errorStringRes),
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    if (uiState.activationSuccess) {
                        Text("تم تفعيل الكود بنجاح.", color = VerdictGreen, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onActivate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_activate_license"),
                        enabled = !uiState.isActivating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandMint,
                            contentColor = NavyBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isActivating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NavyBackground, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = stringResource(R.string.subscribe_activate_btn),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_whatsapp_owner"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGold),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BrandGold))
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.subscribe_whatsapp_owner),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.help_device_id_label), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        Text(uiState.deviceId, color = TextWhite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(uiState.deviceId))
                            Toast.makeText(context, R.string.action_copied, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BrandMint)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDataSentExpanded = !isDataSentExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BrandGold, modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.subscribe_data_sent_title),
                                color = TextWhite,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Icon(
                            imageVector = if (isDataSentExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                    AnimatedVisibility(visible = isDataSentExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = stringResource(R.string.subscribe_data_sent_content),
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
