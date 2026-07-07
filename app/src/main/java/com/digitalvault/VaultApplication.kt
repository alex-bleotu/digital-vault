package com.digitalvault

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.os.Process
import androidx.core.content.getSystemService
import com.digitalvault.core.service.VaultNotifications
import com.digitalvault.core.service.WatchdogWorker

private const val LEGACY_FOREGROUND_NOTIFICATION_ID = 1001

class VaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        VaultNotifications.ensureChannels(this)
        if (isMainProcess()) {
            WatchdogWorker.schedule(this)
        }
        getSystemService<NotificationManager>()?.cancel(LEGACY_FOREGROUND_NOTIFICATION_ID)
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val processName = getSystemService<ActivityManager>()
            ?.runningAppProcesses
            ?.firstOrNull { it.pid == pid }
            ?.processName

        return processName == null || processName == packageName
    }
}
