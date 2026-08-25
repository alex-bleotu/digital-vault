package com.digitalvault.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.digitalvault.core.data.DnsRepository
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.core.vpn.VaultVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            VaultNotifications.ensureChannels(context)
            WatchdogWorker.schedule(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val config = DnsRepository(context.vaultDataStore).config.first()
                    if (config.isVpnBlockingEnabled && VpnService.prepare(context) == null) {
                        VaultVpnService.start(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
