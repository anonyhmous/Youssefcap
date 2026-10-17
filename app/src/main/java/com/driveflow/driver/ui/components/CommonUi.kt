package com.driveflow.driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.ui.theme.BrandGold
import com.driveflow.driver.ui.theme.BrandMint
import com.driveflow.driver.ui.theme.NavySurface
import com.driveflow.driver.ui.theme.NavySurfaceElevated
import com.driveflow.driver.ui.theme.TextMuted
import com.driveflow.driver.ui.theme.TextWhite
import com.driveflow.driver.ui.theme.VerdictGray
import com.driveflow.driver.ui.theme.VerdictOrange
import com.driveflow.driver.ui.theme.VerdictRed

data class Governorate(val id: String, val nameAr: String, val nameEn: String)

val EGYPT_GOVERNORATES = listOf(
    Governorate("cairo", "القاهرة", "Cairo"),
    Governorate("giza", "الجيزة", "Giza"),
    Governorate("alexandria", "الإسكندرية", "Alexandria"),
    Governorate("qalyubia", "القليوبية", "Qalyubia"),
    Governorate("sharqia", "الشرقية", "Sharqia"),
    Governorate("dakahlia", "الدقهلية", "Dakahlia"),
    Governorate("beheira", "البحيرة", "Beheira"),
    Governorate("gharbia", "الغربية", "Gharbia"),
    Governorate("menoufia", "المنوفية", "Menoufia"),
    Governorate("kafr_el_sheikh", "كفر الشيخ", "Kafr El Sheikh"),
    Governorate("damietta", "دمياط", "Damietta"),
    Governorate("port_said", "بورسعيد", "Port Said"),
    Governorate("ismailia", "الإسماعيلية", "Ismailia"),
    Governorate("suez", "السويس", "Suez"),
    Governorate("north_sinai", "شمال سيناء", "North Sinai"),
    Governorate("south_sinai", "جنوب سيناء", "South Sinai"),
    Governorate("faiyum", "الفيوم", "Faiyum"),
    Governorate("beni_suef", "بني سويف", "Beni Suef"),
    Governorate("minya", "المنيا", "Minya"),
    Governorate("asyut", "أسيوط", "Asyut"),
    Governorate("sohag", "سوهاج", "Sohag"),
    Governorate("qena", "قنا", "Qena"),
    Governorate("luxor", "الأقصر", "Luxor"),
    Governorate("aswan", "أسوان", "Aswan"),
    Governorate("red_sea", "البحر الأحمر", "Red Sea"),
    Governorate("new_valley", "الوادي الجديد", "New Valley"),
    Governorate("matruh", "مطروح", "Matruh")
)

val BUNDLED_DISTRICTS: Map<String, List<String>> = mapOf(
    "cairo" to listOf("المعادي", "مدينة نصر", "مصر الجديدة", "التجمع الخامس", "الرحاب", "مدينتي", "الزمالك", "المهندسين", "شبرا", "عين شمس", "حلوان", "المقطم", "شيراتون", "العباسية", "وسط البلد", "الدقي", "العجوزة"),
    "giza" to listOf("الهرم", "فيصل", "الدقي", "العجوزة", "الشيخ زايد", "6 أكتوبر", "الوراق", "إمبابة", "العمرانية", "البدرشين", "الحوامدية"),
    "alexandria" to listOf("سموحة", "سيدي جابر", "محطة الرمل", "ميامي", "العصافرة", "المندرة", "المنتزه", "لوران", "رشدي", "العجمي", "الإبراهيمية", "الشاطبي", "كليوباترا"),
    "qalyubia" to listOf("شبرا الخيمة", "بنها", "قليوب", "الخانكة", "القناطر الخيرية", "طوخ", "العبور")
)

fun normalizeNumericInput(input: String): String {
    val arabicIndicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder()
    for (ch in input.trim()) {
        when {
            ch in '0'..'9' -> sb.append(ch)
            ch in arabicIndicDigits -> sb.append((ch.code - '٠'.code + '0'.code).toChar())
            ch == '.' || ch == ',' || ch == '٫' || ch == '٬' -> sb.append('.')
            ch == '-' -> sb.append('-')
        }
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveFlowTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = TextWhite
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NavySurface
        )
    )
}

@Composable
fun LicenseStatusChip(status: LicenseStatus) {
    val (labelRes, color) = when (status) {
        LicenseStatus.TRIAL -> R.string.status_trial to BrandGold
        LicenseStatus.ACTIVE -> R.string.status_active to BrandMint
        LicenseStatus.FREE -> R.string.status_free to VerdictOrange
        LicenseStatus.REVOKED -> R.string.status_revoked to VerdictRed
        LicenseStatus.UNKNOWN -> R.string.status_unknown to VerdictGray
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = SolidColor(color)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = stringResource(id = labelRes),
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RestrictedSettingsDialog(
    onDismiss: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_restricted_title),
                color = BrandGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_restricted_msg),
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onOpenAppSettings()
            }) {
                Text(stringResource(R.string.guide_open_settings_btn), color = BrandMint, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = TextMuted)
            }
        },
        containerColor = NavySurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}
