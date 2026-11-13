package com.digitalvault.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.digitalvault.MainActivity
import com.digitalvault.R

object VaultNotifications {

    const val STATUS_CHANNEL_ID = "vault_status"
    const val ALERT_CHANNEL_ID = "vault_alert"
    const val ALERT_NOTIFICATION_ID = 1002
    const val VPN_TAKEN_OVER_NOTIFICATION_ID = 1003
    const val EXTRA_RESTORE_VPN = "com.digitalvault.extra.RESTORE_VPN"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        val statusChannel = NotificationChannel(
            STATUS_CHANNEL_ID,
            "Protection status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows that Digital Vault is running."
            setShowBadge(false)
        }
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Protection alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Warns you when a protection stops working."
        }
        manager.createNotificationChannel(statusChannel)
        manager.createNotificationChannel(alertChannel)
    }

    fun showBrokenAlert(context: Context, message: String) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vault_notification)
            .setContentTitle("A protection needs attention")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    fun showVpnTakenOverAlert(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val message = "Another VPN app took over. Tap to turn domain blocking back on."
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vault_notification)
            .setContentTitle("VPN turned off")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, restoreVpn = true))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(VPN_TAKEN_OVER_NOTIFICATION_ID, notification)
    }

    fun clearVpnTakenOverAlert(context: Context) {
        context.getSystemService<NotificationManager>()?.cancel(VPN_TAKEN_OVER_NOTIFICATION_ID)
    }

    fun clearBrokenAlert(context: Context) {
        context.getSystemService<NotificationManager>()?.cancel(ALERT_NOTIFICATION_ID)
    }

    private fun openAppIntent(context: Context, restoreVpn: Boolean = false) =
        androidx.core.app.TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(
                Intent(context, MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_RESTORE_VPN, restoreVpn),
            )
            getPendingIntent(
                if (restoreVpn) 1 else 0,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }
}
