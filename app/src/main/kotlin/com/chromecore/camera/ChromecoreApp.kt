package com.chromecore.camera

import android.app.Application
import com.chromecore.camera.core.AppLog
import com.chromecore.camera.core.CrashHandler

class ChromecoreApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize crash handler
        CrashHandler.init(this)

        // Initialize app log
        AppLog.i("App", "CHROMECORE X started")
        AppLog.d("App", "Device: ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}")
    }
}
