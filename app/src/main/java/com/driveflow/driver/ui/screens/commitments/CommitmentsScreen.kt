package com.driveflow.driver.ui.screens.commitments

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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.ui.components.DriveFlowTopBar
import com.driveflow.driver.ui.theme.NavyBackground
import com.driveflow.driver.ui.theme.NavySurface
import com.driveflow.driver.ui.theme.NavySurfaceElevated
import com.driveflow.driver.ui.theme.OutlineNavy
import com.driveflow.driver.ui.theme.TextMuted
import com.driveflow.driver.ui.theme.TextWhite
import com.driveflow.driver.ui.theme.VerdictRed

private data class Commitment(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

@Composable
fun CommitmentsScreen(
    onBack: () -> Unit
) {
    val commitments = listOf(
        Commitment(R.string.commitments_item1_title, R.string.commitments_item1_desc, Icons.Default.DoNotTouch),
        Commitment(R.string.commitments_item2_title, R.string.commitments_item2_desc, Icons.Default.Cancel),
        Commitment(R.string.commitments_item3_title, R.string.commitments_item3_desc, Icons.Default.MoneyOff),
        Commitment(R.string.commitments_item4_title, R.string.commitments_item4_desc, Icons.Default.Lock),
        Commitment(R.string.commitments_item5_title, R.string.commitments_item5_desc, Icons.Default.Block)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("screen_commitments"),
        containerColor = NavyBackground,
        topBar = {
            DriveFlowTopBar(
                title = stringResource(R.string.commitments_title),
                subtitle = stringResource(R.string.commitments_subtitle),
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            commitments.forEach { item ->
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
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = NavySurfaceElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = VerdictRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(item.titleRes),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(item.descRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
