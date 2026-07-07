package com.digitalvault.core.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object OemAutostartRegistry {

    private val registry: Map<String, List<ComponentName>> = mapOf(
        "xiaomi" to listOf(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        ),
        "redmi" to listOf(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        ),
        "poco" to listOf(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        ),
        "oppo" to listOf(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity",
            ),
        ),
        "realme" to listOf(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity",
            ),
        ),
        "vivo" to listOf(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            ),
        ),
        "huawei" to listOf(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
        ),
        "honor" to listOf(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
        ),
        "oneplus" to listOf(
            ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
            ),
        ),
    )

    fun manufacturer(): String = Build.MANUFACTURER.lowercase()

    fun isSamsung(): Boolean = manufacturer() == "samsung"

    fun needsAutostartStep(context: Context): Boolean = autostartIntent(context) != null

    fun autostartIntent(context: Context): Intent? {
        val candidates = registry[manufacturer()] ?: return null

        return candidates
            .map { componentName -> Intent().setComponent(componentName) }
            .firstOrNull { intent ->
                context.packageManager.resolveActivity(intent, 0) != null
            }
    }
}
