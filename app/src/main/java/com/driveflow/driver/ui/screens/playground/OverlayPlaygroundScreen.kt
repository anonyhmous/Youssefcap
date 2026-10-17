package com.driveflow.driver.ui.screens.playground

import android.graphics.Rect
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.AlertMode
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.overlay.alert.OpportunityAlertManager
import com.driveflow.driver.overlay.model.BarMode
import com.driveflow.driver.overlay.placement.CardMarkerPlacement
import com.driveflow.driver.ui.theme.BrandGold
import com.driveflow.driver.ui.theme.BrandMint
import com.driveflow.driver.ui.theme.NavyBackground
import com.driveflow.driver.ui.theme.NavySurface
import com.driveflow.driver.ui.theme.NavySurfaceElevated
import com.driveflow.driver.ui.theme.NavySurfaceVariant
import com.driveflow.driver.ui.theme.OutlineNavy
import com.driveflow.driver.ui.theme.TextMuted
import com.driveflow.driver.ui.theme.TextWhite
import com.driveflow.driver.ui.theme.toColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayPlaygroundScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSize by remember { mutableStateOf(MarkerSize.NORMAL) }
    var barOpacity by remember { mutableFloatStateOf(67f) }
    var isBarCollapsed by remember { mutableStateOf(false) }
    var selectedBarMode by remember { mutableStateOf(BarMode.VERDICT) }
    var alertTestFeedback by remember { mutableStateOf<String?>(null) }

    val alertManager = remember { OpportunityAlertManager(context) }

    val simulatedRequests = remember {
        listOf(
            SimulatedCard(
                app = RideApp.INDRIVE,
                fare = "135 ج.م",
                distance = "9.5 كم (12 د)",
                route = "المعادي شارع 9 -> التجمع الخامس",
                rating = "4.95 ★ (140 رحلة)",
                tone = Tone.GREEN,
                title = "14.2 ج/كم صافي (+15 ج.م)",
                flags = emptyList()
            ),
            SimulatedCard(
                app = RideApp.UBER,
                fare = "45 ج.م",
                distance = "1.5 كم (بيك اب 4 كم)",
                route = "مصر الجديدة -> ش النزهة",
                rating = "4.70 ★ (12 رحلة)",
                tone = Tone.RED,
                title = "بيك اب بعيد / السعر غير مجدي",
                flags = listOf("PICKUP_FAR_KM")
            ),
            SimulatedCard(
                app = RideApp.DIDI,
                fare = "210 ج.م",
                distance = "28 كم (45 د)",
                route = "المهندسين -> مدينتي",
                rating = "4.88 ★ (85 رحلة)",
                tone = Tone.ORANGE,
                title = "مشوار طويل (+25 كم)",
                flags = listOf("LONG_TRIP_WARN")
            ),
            SimulatedCard(
                app = RideApp.INDRIVE,
                fare = "110 ج.م",
                distance = "7.8 كم",
                route = "الدقي -> مدينة نصر",
                rating = "4.90 ★ (60 رحلة)",
                tone = Tone.GREEN,
                title = "طريق مفتوح لبيتك (Open Route)",
                flags = listOf("OPEN_FOR_ROUTE")
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.playground_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("playground_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavySurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Description
            Text(
                text = stringResource(R.string.playground_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            // 1. Interactive Control Bar Preview Card
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
                        Text(
                            text = "معاينة الشريط العائم (Floating Bar)",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(
                            onClick = { isBarCollapsed = !isBarCollapsed },
                            modifier = Modifier.size(32.dp).testTag("btn_toggle_collapse")
                        ) {
                            Icon(
                                imageVector = if (isBarCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = "Collapse",
                                tint = BrandGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Floating Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = Color(0xFF0E2230).copy(alpha = (barOpacity / 100f).coerceIn(0.2f, 1f)),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF22435A))
                        )
                    ) {
                        if (isBarCollapsed) {
                            // Dot state
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0E2230))
                                        .border(1.dp, BrandGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = BrandGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("الشريط مصغر (اضغط للتوسيع)", color = TextMuted, fontSize = 12.sp)
                            }
                        } else {
                            // Full Bar State
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (selectedBarMode) {
                                                    BarMode.READING -> Color(0xFF64748B)
                                                    BarMode.WAITING_DETAILS -> Color(0xFF64748B)
                                                    BarMode.LICENSE_PROBLEM -> Color(0xFFEF4444)
                                                    BarMode.READER_PAUSED -> Color(0xFFF59E0B)
                                                    BarMode.UPDATE_AVAILABLE -> Color(0xFF22C55E)
                                                    BarMode.VERDICT -> Color(0xFF22C55E)
                                                    BarMode.HIDDEN -> Color.Transparent
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (selectedBarMode) {
                                                BarMode.READING -> stringResource(R.string.overlay_state_reading)
                                                BarMode.WAITING_DETAILS -> stringResource(R.string.overlay_state_waiting_details)
                                                BarMode.LICENSE_PROBLEM -> stringResource(R.string.overlay_state_license_problem)
                                                BarMode.READER_PAUSED -> stringResource(R.string.overlay_state_reader_paused)
                                                BarMode.UPDATE_AVAILABLE -> stringResource(R.string.overlay_state_update_available)
                                                BarMode.VERDICT -> "14.2 ج/كم صافي"
                                                BarMode.HIDDEN -> ""
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Info, contentDescription = null, tint = BrandGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                }

                                if (selectedBarMode == BarMode.VERDICT) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("135 ج.م", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("9.5 كم", color = TextMuted, fontSize = 12.sp)
                                        Text("14.2 صافي/كم", color = BrandMint, fontSize = 12.sp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NavySurfaceVariant)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(stringResource(R.string.overlay_road_easy), color = BrandGold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opacity Slider
                    Text(
                        text = "شفافية الشريط: ${barOpacity.toInt()}%",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = barOpacity,
                        onValueChange = { barOpacity = it },
                        valueRange = 20f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandGold,
                            activeTrackColor = BrandGold,
                            inactiveTrackColor = NavySurfaceElevated
                        ),
                        modifier = Modifier.testTag("slider_playground_opacity")
                    )

                    // Mode chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            BarMode.VERDICT to "تقييم",
                            BarMode.READING to "قراءة",
                            BarMode.LICENSE_PROBLEM to "اشتراك"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = selectedBarMode == mode,
                                onClick = { selectedBarMode = mode },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandGold,
                                    selectedLabelColor = NavyBackground
                                )
                            )
                        }
                    }
                }
            }

            // 2. Marker Size Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "حجم علامة التقييم بجانب الطلبات",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            MarkerSize.SMALL to "صغير (20dp)",
                            MarkerSize.NORMAL to "متوسط (28dp)",
                            MarkerSize.LARGE to "كبير (38dp)"
                        ).forEach { (size, label) ->
                            FilterChip(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandGold,
                                    selectedLabelColor = NavyBackground
                                )
                            )
                        }
                    }
                }
            }

            // 3. Opportunity Alert Test Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val dummyReq = ParsedRequest(
                                app = RideApp.INDRIVE,
                                key = "test_alert_${System.currentTimeMillis()}",
                                source = ReadSource.ACCESSIBILITY,
                                fare = 150.0,
                                currency = "EGP",
                                pickupKm = 1.0,
                                tripKm = 8.0,
                                totalKm = 9.0,
                                pickupMinutes = 4,
                                riderRating = 4.9,
                                riderTrips = 80,
                                requestAgeSeconds = 10,
                                pickupAddress = "المعادي",
                                destinationAddress = "التجمع",
                                extraStops = 0,
                                paymentMethod = "CASH",
                                riderNote = null,
                                offerCount = null,
                                roadState = RoadState.EASY,
                                productName = "inDrive",
                                bounds = Rect(0, 0, 100, 100)
                            )
                            val dummyVerdict = Verdict(
                                tone = Tone.GREEN,
                                reasonCode = "PRICE_OK",
                                titleAr = "طلب لقطة 16.6 ج/كم صافي",
                                titleEn = "Great Trip 16.6 EGP/km",
                                detailAr = "مطابق للشروط",
                                detailEn = "Matches green policy",
                                flags = emptyList(),
                                totalKm = 9.0,
                                grossPerKm = 16.6,
                                netPerKm = 15.0,
                                netProfit = 110.0,
                                suggestedPrice = 160.0
                            )
                            alertManager.executeAlert(dummyReq, dummyVerdict, AlertMode.TONE_VIBRATE)
                            alertTestFeedback = "تم إطلاق الرنين والاهتزاز والإشعار بنجاح!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_test_alert")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = NavyBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.playground_test_alert_btn),
                            fontWeight = FontWeight.Bold,
                            color = NavyBackground
                        )
                    }

                    if (alertTestFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = alertTestFeedback ?: "",
                            color = BrandMint,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 4. Simulated Ride App Cards with Attached Markers
            Text(
                text = stringResource(R.string.playground_simulate_requests),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            simulatedRequests.forEach { simCard ->
                SimulatedRideCardItem(
                    card = simCard,
                    markerSize = selectedSize
                )
            }
        }
    }
}

private data class SimulatedCard(
    val app: RideApp,
    val fare: String,
    val distance: String,
    val route: String,
    val rating: String,
    val tone: Tone,
    val title: String,
    val flags: List<String>
)

@Composable
private fun SimulatedRideCardItem(
    card: SimulatedCard,
    markerSize: MarkerSize
) {
    val markerSizeDp = when (markerSize) {
        MarkerSize.SMALL -> 20.dp
        MarkerSize.NORMAL -> 28.dp
        MarkerSize.LARGE -> 38.dp
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Card marker beside the card
        Box(
            modifier = Modifier
                .size(markerSizeDp)
                .clip(CircleShape)
                .background(
                    if (card.flags.contains("OPEN_FOR_ROUTE")) Color(0xFF4ADE80)
                    else card.tone.toColor()
                )
                .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (card.flags.contains("OPEN_FOR_ROUTE")) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(markerSizeDp * 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Request Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(OutlineNavy)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${card.app.name} • ${card.distance}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = card.fare,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.route,
                    color = TextWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = card.rating, color = BrandGold, fontSize = 11.sp)
                    Text(text = card.title, color = card.tone.toColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
