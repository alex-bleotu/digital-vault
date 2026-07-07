package com.digitalvault.core.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.digitalvault.R

class VaultDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.device_admin_disable_warning)

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context, VaultDeviceAdminReceiver::class.java)
    }
}
