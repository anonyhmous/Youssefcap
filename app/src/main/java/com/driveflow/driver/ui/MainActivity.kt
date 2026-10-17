package com.driveflow.driver.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.locale.AppLocaleManager
import com.driveflow.driver.ui.navigation.DriveFlowNavHost
import com.driveflow.driver.ui.theme.DriveFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsStore = AppGraph.settingsStore
            val language by settingsStore.languageFlow.collectAsStateWithLifecycle(initialValue = "ar")

            DriveFlowTheme(forceRtl = AppLocaleManager.isRtl(language)) {
                DriveFlowNavHost()
            }
        }
    }
}
