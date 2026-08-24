package com.digitalvault.core.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.digitalvault.core.data.DnsRepository
import com.digitalvault.core.data.HealthRepository
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.core.permissions.SetupPermissions
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class WatchdogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val permissions = SetupPermissions(context)

        HealthRepository(context.vaultDataStore).recordCheck(Instant.now())

        val accessibilityLive = permissions.isAccessibilityEnabled()
        val overlayLive = permissions.isOverlayGranted()
        val shouldVpnBeActive = DnsRepository(context.vaultDataStore).config.first().isVpnBlockingEnabled
        val vpnLive = permissions.isVpnActive()

        when {
            !accessibilityLive ->
                VaultNotifications.showBrokenAlert(
                    context,
                    "The blocking engine is off. Open Digital Vault and turn Accessibility back on.",
                )

            !overlayLive ->
                VaultNotifications.showBrokenAlert(
                    context,
                    "The block screen can't appear. Open Digital Vault and grant overlay access.",
                )

            shouldVpnBeActive && !vpnLive ->
                VaultNotifications.showBrokenAlert(
                    context,
                    "DNS blocking stopped running. Open Digital Vault's Shield tab to turn it back on.",
                )

            else -> VaultNotifications.clearBrokenAlert(context)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "vault_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
