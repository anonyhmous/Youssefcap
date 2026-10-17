package com.driveflow.driver.ui.screens.areas

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.driveflow.driver.ui.theme.VerdictOrange
import com.driveflow.driver.ui.theme.VerdictRed

@Composable
fun AreasScreen(
    uiState: AreasUiState,
    onAddArea: (String, Boolean) -> Unit,
    onRemoveArea: (String, Boolean) -> Unit,
    onMoveArea: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var areaToDelete by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var areaToMove by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val isAvoidedTab = selectedTab == 0
    val currentList = if (isAvoidedTab) uiState.avoidedAreas else uiState.preferredAreas

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_areas"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.areas_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.governorate.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VerdictOrange.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(VerdictOrange)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = VerdictOrange)
                        Text(
                            text = stringResource(R.string.areas_no_gov_warning),
                            color = TextWhite,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavySurface,
                contentColor = TextWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (isAvoidedTab) VerdictRed else BrandMint
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "${stringResource(R.string.areas_avoided_tab)} (${uiState.avoidedAreas.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) VerdictRed else TextMuted
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "${stringResource(R.string.areas_preferred_tab)} (${uiState.preferredAreas.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) BrandMint else TextMuted
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(R.string.areas_add_hint), fontSize = 13.sp, color = TextMuted) },
                    modifier = Modifier.weight(1f).testTag("input_area_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = if (isAvoidedTab) VerdictRed else BrandMint,
                        unfocusedBorderColor = OutlineNavy,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface
                    )
                )
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onAddArea(inputText, isAvoidedTab)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.height(52.dp).testTag("btn_add_area"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAvoidedTab) VerdictRed else BrandMint,
                        contentColor = NavyBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_add), fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.suggestions.isNotEmpty()) {
                Column {
                    Text(
                        text = "أحياء مقترحة بمحافظتك:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(uiState.suggestions) { suggestion ->
                            Surface(
                                modifier = Modifier.clickable {
                                    onAddArea(suggestion, isAvoidedTab)
                                },
                                color = NavySurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = BrandGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(suggestion, color = TextWhite, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(if (isAvoidedTab) R.string.areas_empty_avoided else R.string.areas_empty_preferred),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentList) { area ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = area,
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { areaToMove = area to isAvoidedTab }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.CompareArrows,
                                            contentDescription = "Move",
                                            tint = BrandGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { areaToDelete = area to isAvoidedTab }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = VerdictRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (areaToDelete != null) {
        val (area, isAvoided) = areaToDelete!!
        AlertDialog(
            onDismissRequest = { areaToDelete = null },
            title = {
                Text(stringResource(R.string.action_delete), color = VerdictRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "${stringResource(R.string.areas_confirm_delete)} ($area)",
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveArea(area, isAvoided)
                    areaToDelete = null
                }) {
                    Text(stringResource(R.string.action_confirm), color = VerdictRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { areaToDelete = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextMuted)
                }
            },
            containerColor = NavySurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (areaToMove != null) {
        val (area, fromAvoided) = areaToMove!!
        AlertDialog(
            onDismissRequest = { areaToMove = null },
            title = {
                Text(stringResource(R.string.action_confirm), color = BrandGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "${stringResource(R.string.areas_confirm_move)} ($area)",
                    color = TextWhite
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onMoveArea(area, fromAvoided)
                    areaToMove = null
                }) {
                    Text(stringResource(R.string.action_confirm), color = BrandMint)
                }
            },
            dismissButton = {
                TextButton(onClick = { areaToMove = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextMuted)
                }
            },
            containerColor = NavySurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
