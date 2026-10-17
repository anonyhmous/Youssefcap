package com.driveflow.driver.ui.screens.homeway

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.ui.components.DriveFlowTopBar
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
fun HomeWayScreen(
    state: HomeWayFormState,
    onUpdateForm: ((HomeWayFormState) -> HomeWayFormState) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_homeway"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.homeway_title),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = NavySurfaceElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (state.enabled) BrandGold else TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.homeway_switch),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = if (state.enabled) "الوضع مفعل حالياً" else "الوضع متوقف",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.enabled) BrandMint else TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { checked ->
                            onUpdateForm { it.copy(enabled = checked) }
                        },
                        modifier = Modifier.testTag("switch_homeway_master"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyBackground,
                            checkedTrackColor = BrandGold
                        )
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
                        text = "اختر نوع الوجهة:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = state.placeMode == "HOME",
                            onClick = { onUpdateForm { it.copy(placeMode = "HOME") } },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            label = { Text(stringResource(R.string.homeway_mode_home)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandGold,
                                selectedLabelColor = NavyBackground
                            )
                        )
                        FilterChip(
                            selected = state.placeMode == "PLACE",
                            onClick = { onUpdateForm { it.copy(placeMode = "PLACE") } },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            label = { Text(stringResource(R.string.homeway_mode_custom)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandMint,
                                selectedLabelColor = NavyBackground
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "الإحداثيات أو الرابط (خط الطول والعرض):",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.locationInput,
                        onValueChange = { onUpdateForm { s -> s.copy(locationInput = it) } },
                        placeholder = { Text(stringResource(R.string.homeway_input_hint), fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("input_homeway_location"),
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "اسم الوجهة لتسهيل التمييز:",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.label,
                        onValueChange = { onUpdateForm { s -> s.copy(label = it) } },
                        placeholder = { Text(stringResource(R.string.homeway_label_hint), fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("input_homeway_label"),
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_save_homeway"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandMint,
                                contentColor = NavyBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onClear,
                            modifier = Modifier.height(48.dp).testTag("btn_clear_homeway"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = VerdictRed
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(VerdictRed)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.homeway_clear_btn))
                        }
                    }
                    if (state.isSavedSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("تم الحفظ بنجاح", color = VerdictGreen, style = MaterialTheme.typography.bodySmall)
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
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BrandGold, modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(R.string.homeway_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}
