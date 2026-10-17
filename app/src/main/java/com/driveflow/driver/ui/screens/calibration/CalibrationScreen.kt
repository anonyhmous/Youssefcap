package com.driveflow.driver.ui.screens.calibration

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driveflow.driver.R
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.ocr.LastScreenshotHolder
import com.driveflow.driver.ocr.ScreenKind
import com.driveflow.driver.ocr.model.CropRectF
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

@Composable
fun CalibrationScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalibrationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val screenshot = LastScreenshotHolder.lastBitmap

    Scaffold(
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.calibration_title),
                onBack = onNavigateBack
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "هذه الشاشة مخصصة لمعايرة إحداثيات ومربعات التعرف الضوئي على النصوص لكل تطبيق ونوع شاشة.",
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Text(
                text = "اختر التطبيق (Ride App)",
                color = BrandGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(RideApp.INDRIVE, RideApp.DIDI, RideApp.UBER).forEach { app ->
                    FilterChip(
                        selected = uiState.selectedApp == app,
                        onClick = { viewModel.selectApp(app) },
                        label = {
                            Text(
                                text = when (app) {
                                    RideApp.INDRIVE -> "inDrive"
                                    RideApp.DIDI -> "DiDi"
                                    RideApp.UBER -> "Uber (قريباً)"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandGold,
                            selectedLabelColor = NavyBackground,
                            containerColor = NavySurface,
                            labelColor = TextWhite
                        )
                    )
                }
            }

            Text(
                text = "الحزمة المحددة: ${uiState.detectedPackageName}",
                color = TextMuted,
                fontSize = 12.sp
            )

            Text(
                text = "نوع الشاشة (Screen Kind)",
                color = BrandMint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedKind == ScreenKind.LIST_CARD,
                    onClick = { viewModel.selectKind(ScreenKind.LIST_CARD) },
                    label = { Text("قائمة الطلبات (LIST)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandMint,
                        selectedLabelColor = NavyBackground,
                        containerColor = NavySurface,
                        labelColor = TextWhite
                    )
                )
                FilterChip(
                    selected = uiState.selectedKind == ScreenKind.DETAIL,
                    onClick = { viewModel.selectKind(ScreenKind.DETAIL) },
                    label = { Text("تفاصيل الطلب (DETAIL)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandMint,
                        selectedLabelColor = NavyBackground,
                        containerColor = NavySurface,
                        labelColor = TextWhite
                    )
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (screenshot != null && !screenshot.isRecycled) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = screenshot.asImageBitmap(),
                            contentDescription = "Screen preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "لا توجد لقطة شاشة مخزنة في الذاكرة حالياً",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "افتح تطبيق التوصيل ودع المساعد يقرأ طلباً",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "المربع المحدد (OCR Box)",
                color = BrandGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            val boxes = listOf(
                "fareBox" to "السعر (Fare)",
                "pickupBox" to "الوصول (Pickup)",
                "tripBox" to "المشوار (Trip Km)",
                "ratingBox" to "التقييم (Rating)",
                "productBox" to "النوع (Product)"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState(), enabled = false),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                boxes.forEach { (boxKey, label) ->
                    FilterChip(
                        selected = uiState.selectedBox == boxKey,
                        onClick = { viewModel.selectBox(boxKey) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandGold,
                            selectedLabelColor = NavyBackground,
                            containerColor = NavySurfaceElevated,
                            labelColor = TextWhite
                        )
                    )
                }
            }

            val currentBox = when (uiState.selectedBox) {
                "fareBox" -> uiState.currentCrops?.fareBox
                "pickupBox" -> uiState.currentCrops?.pickupBox
                "tripBox" -> uiState.currentCrops?.tripBox
                "ratingBox" -> uiState.currentCrops?.ratingBox
                "productBox" -> uiState.currentCrops?.productBox
                else -> null
            } ?: CropRectF(0.08f, 0.58f, 0.92f, 0.68f)

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نسب الإحداثيات (من 0% إلى 100%):",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Left: ${(currentBox.left * 100).toInt()}%", color = TextMuted, fontSize = 12.sp)
                    Slider(
                        value = currentBox.left,
                        onValueChange = { viewModel.updateSelectedBox(it, currentBox.top, currentBox.right, currentBox.bottom) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = BrandGold, activeTrackColor = BrandGold)
                    )
                    Text("Top: ${(currentBox.top * 100).toInt()}%", color = TextMuted, fontSize = 12.sp)
                    Slider(
                        value = currentBox.top,
                        onValueChange = { viewModel.updateSelectedBox(currentBox.left, it, currentBox.right, currentBox.bottom) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = BrandGold, activeTrackColor = BrandGold)
                    )
                    Text("Right: ${(currentBox.right * 100).toInt()}%", color = TextMuted, fontSize = 12.sp)
                    Slider(
                        value = currentBox.right,
                        onValueChange = { viewModel.updateSelectedBox(currentBox.left, currentBox.top, it, currentBox.bottom) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = BrandGold, activeTrackColor = BrandGold)
                    )
                    Text("Bottom: ${(currentBox.bottom * 100).toInt()}%", color = TextMuted, fontSize = 12.sp)
                    Slider(
                        value = currentBox.bottom,
                        onValueChange = { viewModel.updateSelectedBox(currentBox.left, currentBox.top, currentBox.right, it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = BrandGold, activeTrackColor = BrandGold)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandMint.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نتائج القراءة الحية (Live OCR):",
                            color = BrandMint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { viewModel.triggerLiveOcr() },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = BrandMint, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديث القراءة", color = BrandMint, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val activeOutput = uiState.liveOcrOutputs[uiState.selectedBox] ?: "(لا توجد قراءة)"
                    Text(
                        text = "النص المقروء [${uiState.selectedBox}]: $activeOutput",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تحليل حالة الطريق (RoadState)",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "فحص لون خط سير الرحلة على الخريطة",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = uiState.experimentalRoadState,
                        onCheckedChange = { viewModel.toggleExperimentalRoadState(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BrandGold,
                            checkedTrackColor = BrandGold.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val json = viewModel.copyConfigJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("DriveFlow OCR Config", json))
                        Toast.makeText(context, "تم نسخ كود JSON", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceElevated)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ JSON", color = TextWhite, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        viewModel.saveCalibration()
                        Toast.makeText(context, "تم حفظ الإحداثيات", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdictGreen)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = NavyBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ المعايرة", color = NavyBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
