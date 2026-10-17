package com.driveflow.driver.ui.screens.contact

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.ui.components.DriveFlowTopBar
import com.driveflow.driver.ui.components.EGYPT_GOVERNORATES
import com.driveflow.driver.ui.components.normalizeNumericInput
import com.driveflow.driver.ui.theme.BrandGold
import com.driveflow.driver.ui.theme.BrandMint
import com.driveflow.driver.ui.theme.NavyBackground
import com.driveflow.driver.ui.theme.NavySurface
import com.driveflow.driver.ui.theme.NavySurfaceElevated
import com.driveflow.driver.ui.theme.OutlineNavy
import com.driveflow.driver.ui.theme.TextMuted
import com.driveflow.driver.ui.theme.TextWhite
import com.driveflow.driver.ui.theme.VerdictRed

@Composable
fun ContactScreen(
    initialName: String = "",
    initialPhone: String = "",
    initialGovId: String? = "cairo",
    isArabic: Boolean = true,
    onSubmit: (name: String, phone: String, govId: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var selectedCountryCode by remember { mutableStateOf("+20") }
    var countryCodeExpanded by remember { mutableStateOf(false) }
    var selectedGovId by remember { mutableStateOf(initialGovId ?: "cairo") }
    var govExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val countryCodes = listOf("+20", "+966", "+971", "+965", "+974", "+962")
    val selectedGov = EGYPT_GOVERNORATES.find { it.id == selectedGovId } ?: EGYPT_GOVERNORATES.first()

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_contact"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.contact_title),
                subtitle = stringResource(R.string.contact_subtitle)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.contact_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.contact_name_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isLetter() || ch.isWhitespace() }
                        name = filtered
                        nameError = null
                    },
                    placeholder = { Text(stringResource(R.string.contact_name_hint), color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandGold)
                    },
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(nameError!!, color = VerdictRed)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BrandGold,
                        unfocusedBorderColor = OutlineNavy,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.contact_phone_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .height(56.dp)
                                .clickable { countryCodeExpanded = true },
                            color = NavySurface,
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCountryCode,
                                    color = BrandMint,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = countryCodeExpanded,
                            onDismissRequest = { countryCodeExpanded = false },
                            modifier = Modifier.background(NavySurfaceElevated)
                        ) {
                            countryCodes.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code, color = TextWhite) },
                                    onClick = {
                                        selectedCountryCode = code
                                        countryCodeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            val normalized = normalizeNumericInput(it).filter { ch -> ch.isDigit() }
                            phone = normalized
                            phoneError = null
                        },
                        placeholder = { Text(stringResource(R.string.contact_phone_hint), color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = BrandMint)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        supportingText = {
                            if (phoneError != null) {
                                Text(phoneError!!, color = VerdictRed)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_contact_phone"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandMint,
                            unfocusedBorderColor = OutlineNavy,
                            focusedContainerColor = NavySurface,
                            unfocusedContainerColor = NavySurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.contact_gov_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { govExpanded = true }
                            .testTag("spinner_governorate"),
                        color = NavySurface,
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandGold)
                                Text(
                                    text = if (isArabic) selectedGov.nameAr else selectedGov.nameEn,
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                        }
                    }
                    DropdownMenu(
                        expanded = govExpanded,
                        onDismissRequest = { govExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(NavySurfaceElevated)
                    ) {
                        EGYPT_GOVERNORATES.forEach { gov ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isArabic) gov.nameAr else gov.nameEn,
                                        color = if (gov.id == selectedGovId) BrandGold else TextWhite,
                                        fontWeight = if (gov.id == selectedGovId) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedGovId = gov.id
                                    govExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val errNameEmpty = stringResource(R.string.error_name_empty)
            val errPhoneInvalid = stringResource(R.string.error_phone_invalid)

            Button(
                onClick = {
                    var hasError = false
                    if (name.trim().length < 2) {
                        nameError = errNameEmpty
                        hasError = true
                    }
                    if (phone.trim().length < 9) {
                        phoneError = errPhoneInvalid
                        hasError = true
                    }
                    if (!hasError) {
                        val fullPhone = "$selectedCountryCode${phone.trim()}"
                        onSubmit(name.trim(), fullPhone, selectedGovId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_contact_submit"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGold,
                    contentColor = NavyBackground
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.contact_submit_btn),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
