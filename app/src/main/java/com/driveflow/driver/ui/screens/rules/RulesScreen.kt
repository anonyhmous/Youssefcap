package com.driveflow.driver.ui.screens.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.StopsMode
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
import com.driveflow.driver.ui.theme.VerdictRed

@Composable
fun RulesScreen(
    state: RulesFormState,
    onUpdateForm: ((RulesFormState) -> RulesFormState) -> Unit,
    onAddBannedWord: (String) -> Unit,
    onRemoveBannedWord: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val expandedSections = remember {
        mutableStateMapOf(
            0 to true,
            1 to false,
            2 to false,
            3 to false,
            4 to false,
            5 to false,
            6 to false
        )
    }
    var newBannedWord by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_rules"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.rules_screen_title),
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                color = NavySurface,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = VerdictRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (state.isSavedSuccess) {
                        Text(
                            text = stringResource(R.string.rules_save_success),
                            color = VerdictGreen,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_save_rules"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandMint,
                            contentColor = NavyBackground
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_save),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // (a) Section 1: Country & Price
            val sec1Summary = "${state.minPricePerKm.ifBlank { "0" }} ${state.currency}/${state.distanceUnit.name}"
            CollapsibleSectionCard(
                title = stringResource(R.string.rules_sec_country_price),
                summary = sec1Summary,
                icon = Icons.Default.LocalAtm,
                iconTint = BrandGold,
                isExpanded = expandedSections[0] == true,
                onToggle = { expandedSections[0] = !(expandedSections[0] ?: false) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StyledTextField(
                        label = stringResource(R.string.rules_min_price_label),
                        value = state.minPricePerKm,
                        onValueChange = { str -> onUpdateForm { it.copy(minPricePerKm = str) } },
                        keyboardType = KeyboardType.Decimal
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledTextField(
                            label = stringResource(R.string.rules_currency_label),
                            value = state.currency,
                            onValueChange = { str -> onUpdateForm { it.copy(currency = str) } },
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.rules_dist_unit_label), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DistanceUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = state.distanceUnit == unit,
                                        onClick = { onUpdateForm { it.copy(distanceUnit = unit) } },
                                        label = { Text(unit.name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandGold,
                                            selectedLabelColor = NavyBackground
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // (b) Section 2: Distance limits
            val sec2Summary = if (state.maxPickupKm.isNotBlank() || state.maxTotalKm.isNotBlank()) {
                "وصول: ${state.maxPickupKm.ifBlank { "بلا حد" }} | كلي: ${state.maxTotalKm.ifBlank { "بلا حد" }}"
            } else {
                "بدون حدود قصوى"
            }
            CollapsibleSectionCard(
                title = stringResource(R.string.rules_sec_distance),
                summary = sec2Summary,
                icon = Icons.Default.Straighten,
                iconTint = BrandMint,
                isExpanded = expandedSections[1] == true,
                onToggle = { expandedSections[1] = !(expandedSections[1] ?: false) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledTextField(
                            label = stringResource(R.string.rules_min_total_km),
                            value = state.minTotalKm,
                            onValueChange = { str -> onUpdateForm { it.copy(minTotalKm = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            label = stringResource(R.string.rules_max_total_km),
                            value = state.maxTotalKm,
                            onValueChange = { str -> onUpdateForm { it.copy(maxTotalKm = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledTextField(
                            label = stringResource(R.string.rules_max_trip_km),
                            value = state.maxTripKm,
                            onValueChange = { str -> onUpdateForm { it.copy(maxTripKm = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            label = stringResource(R.string.rules_max_pickup_km),
                            value = state.maxPickupKm,
                            onValueChange = { str -> onUpdateForm { it.copy(maxPickupKm = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledTextField(
                            label = stringResource(R.string.rules_max_pickup_minutes),
                            value = state.maxPickupMinutes,
                            onValueChange = { str -> onUpdateForm { it.copy(maxPickupMinutes = str) } },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            label = stringResource(R.string.rules_free_pickup_km),
                            value = state.freePickupKm,
                            onValueChange = { str -> onUpdateForm { it.copy(freePickupKm = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // (c) Section 3: Rider & Payment
            val sec3Summary = "تقييم: ${state.minRiderRating.ifBlank { "0" }}+ | ${state.paymentMode}"
            CollapsibleSectionCard(
                title = stringResource(R.string.rules_sec_rider_payment),
                summary = sec3Summary,
                icon = Icons.Default.Person,
                iconTint = BrandGold,
                isExpanded = expandedSections[2] == true,
                onToggle = { expandedSections[2] = !(expandedSections[2] ?: false) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledTextField(
                            label = stringResource(R.string.rules_min_rider_rating),
                            value = state.minRiderRating,
                            onValueChange = { str -> onUpdateForm { it.copy(minRiderRating = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            label = stringResource(R.string.rules_min_rider_trips),
                            value = state.minRiderTrips,
                            onValueChange = { str -> onUpdateForm { it.copy(minRiderTrips = str) } },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    StyledTextField(
                        label = stringResource(R.string.rules_max_request_age),
                        value = state.maxRequestAgeMinutes,
                        onValueChange = { str -> onUpdateForm { it.copy(maxRequestAgeMinutes = str) } },
                        keyboardType = KeyboardType.Number
                    )
                    Text(stringResource(R.string.rules_payment_allowed), color = TextWhite, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.paymentMode == "ALL",
                            onClick = { onUpdateForm { it.copy(paymentMode = "ALL") } },
                            label = { Text(stringResource(R.string.rules_payment_all)) }
                        )
                        FilterChip(
                            selected = state.paymentMode == "CASH",
                            onClick = { onUpdateForm { it.copy(paymentMode = "CASH") } },
                            label = { Text(stringResource(R.string.rules_payment_cash)) }
                        )
                    }
                    Text(stringResource(R.string.rules_banned_words_label), color = TextWhite, style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newBannedWord,
                            onValueChange = { newBannedWord = it },
                            placeholder = { Text(stringResource(R.string.rules_banned_words_hint), fontSize = 12.sp, color = TextMuted) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = BrandGold,
                                unfocusedBorderColor = OutlineNavy,
                                focusedContainerColor = NavySurfaceVariant,
                                unfocusedContainerColor = NavySurfaceVariant
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newBannedWord.isNotBlank()) {
                                    onAddBannedWord(newBannedWord)
                                    newBannedWord = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BrandMint)
                        }
                    }
                    if (state.bannedNoteWords.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.bannedNoteWords) { word ->
                                Surface(
                                    color = VerdictRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(VerdictRed)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(word, color = TextWhite, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp).clickable { onRemoveBannedWord(word) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // (d) Section 4: Stops and Long trips
            val sec4Summary = "وقفات: ${state.stopsMode.name} | سفر: ${state.longTripMode.name}"
            CollapsibleSectionCard(
                title = stringResource(R.string.rules_sec_stops_long),
                summary = sec4Summary,
                icon = Icons.Default.Map,
                iconTint = BrandMint,
                isExpanded = expandedSections[3] == true,
                onToggle = { expandedSections[3] = !(expandedSections[3] ?: false) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.rules_stops_mode), color = TextWhite, style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(StopsMode.entries) { mode ->
                            FilterChip(
                                selected = state.stopsMode == mode,
                                onClick = { onUpdateForm { it.copy(stopsMode = mode) } },
                                label = { Text(mode.name, fontSize = 12.sp) }
                            )
                        }
                    }
                    StyledTextField(
                        label = stringResource(R.string.rules_stop_fee),
                        value = state.stopFee,
                        onValueChange = { str -> onUpdateForm { it.copy(stopFee = str) } },
                        keyboardType = KeyboardType.Decimal
                    )
                    Text(stringResource(R.string.rules_long_trip_mode), color = TextWhite, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LongTripMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.longTripMode == mode,
                                onClick = { onUpdateForm { it.copy(longTripMode = mode) } },
                                label = { Text(mode.name, fontSize = 12.sp) }
                            )
                        }
                    }
                    StyledTextField(
                        label = stringResource(R.string.rules_long_trip_km),
                        value = state.longTripKm,
                        onValueChange = { str -> onUpdateForm { it.copy(longTripKm = str) } },
                        keyboardType = KeyboardType.Decimal
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.rules_warn_uber_saver), color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = state.warnOnUberSaver,
                            onCheckedChange = { checked -> onUpdateForm { it.copy(warnOnUberSaver = checked) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandGold)
                        )
                    }
                }
            }

            // (e) Section 5: Car Cost & Commission
            val sec5Summary = if (state.fuelCostPerKm.isNotBlank() || state.upkeepCostPerKm.isNotBlank()) {
                "تكلفة: ${(state.fuelCostPerKm.toDoubleOrNull() ?: 0.0) + (state.upkeepCostPerKm.toDoubleOrNull() ?: 0.0)} ج/كم"
            } else {
                "بدون خصم تكلفة سيارة"
            }
            CollapsibleSectionCard(
                title = stringResource(R.string.rules_sec_car_cost),
                summary = sec5Summary,
                icon = Icons.Default.DirectionsCar,
                iconTint = BrandGold,
                isExpanded = expandedSections[4] == true,
                onToggle = { expandedSections[4] = !(expandedSections[4] ?: false) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledTextField(
                            label = stringResource(R.string.rules_fuel_cost),
                            value = state.fuelCostPerKm,
                            onValueChange = { str -> onUpdateForm { it.copy(fuelCostPerKm = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            label = stringResource(R.string.rules_upkeep_cost),
                            value = state.upkeepCostPerKm,
                            onValueChange = { str -> onUpdateForm { it.copy(upkeepCostPerKm = str) } },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.rules_basis_net), color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = state.basisNet,
                            onCheckedChange = { checked -> onUpdateForm { it.copy(basisNet = checked) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandMint)
                        )
                    }
                    Text("عمولة التطبيقات (%):", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    RideApp.entries.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(app.code.uppercase(), color = TextWhite, modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold)
                            StyledTextField(
                                label = "%",
                                value = state.commissionPct[app] ?: "",
                                onValueChange = { pct ->
                                    val map = state.commissionPct.toMutableMap()
                                    map[app] = pct
                                    onUpdateForm { it.copy(commissionPct = map) }
                                },
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.commissionEnabled[app] == true,
                                onCheckedChange = { en ->
                                    val map = state.commissionEnabled.toMutableMap()
                                    map[app] = en
                                    onUpdateForm { it.copy(commissionEnabled = map) }
                                }
                            )
                        }
                    }
                }
            }

            // (f) Section 6: Negotiation
            val sec6Summary = "خطوة: ${state.negotiationStep} ${state.currency} | أقصى عرض: ${state.maxOffer.ifBlank { "بلا حد" }}"
            CollapsibleSectionCard(
                title = stringResource(R.string.rules_sec_negotiation),
                summary = sec6Summary,
                icon = Icons.Default.Handshake,
                iconTint = BrandMint,
                isExpanded = expandedSections[5] == true,
                onToggle = { expandedSections[5] = !(expandedSections[5] ?: false) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyledTextField(
                        label = stringResource(R.string.rules_negotiation_step),
                        value = state.negotiationStep,
                        onValueChange = { str -> onUpdateForm { it.copy(negotiationStep = str) } },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    StyledTextField(
                        label = stringResource(R.string.rules_max_offer),
                        value = state.maxOffer,
                        onValueChange = { str -> onUpdateForm { it.copy(maxOffer = str) } },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // (g) Section 7: Per App Toggles
            val sec7Summary = "مفعل: " + RideApp.entries.filter { state.appEnabled[it] == true }.joinToString { it.code }
            CollapsibleSectionCard(
                title = stringResource(R.string.rules_sec_apps),
                summary = sec7Summary,
                icon = Icons.Default.DirectionsCar,
                iconTint = BrandGold,
                isExpanded = expandedSections[6] == true,
                onToggle = { expandedSections[6] = !(expandedSections[6] ?: false) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RideApp.entries.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.code.uppercase(), color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = state.appEnabled[app] == true,
                                onCheckedChange = { en ->
                                    val map = state.appEnabled.toMutableMap()
                                    map[app] = en
                                    onUpdateForm { it.copy(appEnabled = map) }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = BrandMint)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CollapsibleSectionCard(
    title: String,
    summary: String,
    icon: ImageVector,
    iconTint: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextMuted
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun StyledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(text = label, color = TextMuted, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = BrandGold,
                unfocusedBorderColor = OutlineNavy,
                focusedContainerColor = NavySurfaceVariant,
                unfocusedContainerColor = NavySurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
    }
}
