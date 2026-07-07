package com.digitalvault.core.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.digitalvault.core.accessibility.VaultAccessibilityService
import com.digitalvault.core.admin.VaultDeviceAdminReceiver

class SetupPermissions(private val context: Context) {

    fun isOverlayGranted(): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityEnabled(): Boolean {
        val expected = VaultAccessibilityService.componentName(context.packageName)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun isDeviceAdminActive(): Boolean {
        val devicePolicyManager = context.getSystemService<android.app.admin.DevicePolicyManager>() ?: return false

        return devicePolicyManager.isAdminActive(VaultDeviceAdminReceiver.componentName(context))
    }

    fun isBatteryExempted(): Boolean {
        val powerManager = context.getSystemService<PowerManager>() ?: return false

        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun overlayIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri())

    fun accessibilityIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun deviceAdminIntent(explanation: String): Intent =
        Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                VaultDeviceAdminReceiver.componentName(context),
            )
            .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)

    fun batteryIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri())

    fun batterySettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun appSettingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri())

    private fun packageUri(): Uri = "package:${context.packageName}".toUri()
}
