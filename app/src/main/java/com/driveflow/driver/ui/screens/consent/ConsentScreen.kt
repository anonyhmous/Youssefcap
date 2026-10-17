package com.driveflow.driver.ui.screens.consent

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class ConsentPoint(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val iconTint: Color
)

@Composable
fun ConsentScreen(
    onAgree: () -> Unit,
    onReadPolicy: () -> Unit
) {
    val points = listOf(
        ConsentPoint(R.string.consent_point1_title, R.string.consent_point1_desc, Icons.Default.Info, BrandMint),
        ConsentPoint(R.string.consent_point2_title, R.string.consent_point2_desc, Icons.Default.TouchApp, BrandGold),
        ConsentPoint(R.string.consent_point3_title, R.string.consent_point3_desc, Icons.Default.Lock, BrandMint),
        ConsentPoint(R.string.consent_point4_title, R.string.consent_point4_desc, Icons.Default.Report, BrandGold),
        ConsentPoint(R.string.consent_point5_title, R.string.consent_point5_desc, Icons.Default.Warning, VerdictOrange)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_consent"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.consent_title),
                subtitle = stringResource(R.string.consent_subtitle)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            points.forEach { point ->
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
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = NavySurfaceElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = point.icon,
                                    contentDescription = null,
                                    tint = point.iconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(point.titleRes),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(point.descRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAgree,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_consent_agree"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandMint,
                    contentColor = NavyBackground
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.consent_agree_btn),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = onReadPolicy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_consent_read_policy"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BrandGold
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BrandGold)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.consent_read_policy_btn),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
