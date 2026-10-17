package com.driveflow.driver

import android.app.Application
import com.driveflow.driver.core.di.AppGraph

class DriveFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
