package com.digitalvault.ui.rules

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.core.accessibility.matcher.SurfaceMatchers
import com.digitalvault.core.data.RulesRepository
import com.digitalvault.core.data.model.AppRule
import com.digitalvault.core.data.model.BlockMode
import com.digitalvault.core.data.vaultDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val APP_ICON_SIZE_PX = 96

val POPULAR_APP_PACKAGES = listOf(
    "com.android.chrome",
    "com.instagram.android",
    "com.zhiliaoapp.musically",
    "com.google.android.youtube",
    "app.rvx.android.youtube",
    "com.facebook.katana",
    "com.snapchat.android",
    "com.twitter.android",
    "com.reddit.frontpage",
    "com.whatsapp",
    "com.facebook.orca",
    "org.telegram.messenger",
    "com.discord",
    "com.netflix.mediaclient",
    "com.spotify.music",
    "tv.twitch.android.app",
    "com.pinterest",
    "com.linkedin.android",
)

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

data class RulesUiState(
    val rules: List<AppRule> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoadingApps: Boolean = false,
)

class RulesViewModel(application: Application) : AndroidViewModel(application) {

    private val rulesRepository = RulesRepository(application.vaultDataStore)

    var uiState by mutableStateOf(RulesUiState())
        private set

    init {
        viewModelScope.launch {
            rulesRepository.rules.collect { rules ->
                uiState = uiState.copy(rules = rules)
            }
        }
    }

    fun loadInstalledApps() {
        if (uiState.installedApps.isNotEmpty() || uiState.isLoadingApps) {
            return
        }
        uiState = uiState.copy(isLoadingApps = true)
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { queryLaunchableApps() }
            uiState = uiState.copy(installedApps = apps, isLoadingApps = false)
        }
    }

    fun blockApp(packageName: String) {
        viewModelScope.launch {
            val rule = if (SurfaceMatchers.supportsSurfaceBlock(packageName)) {
                AppRule(
                    packageName = packageName,
                    mode = BlockMode.SURFACE_BLOCK,
                    targetSurfaces = SurfaceMatchers.defaultSurfaceIds(packageName),
                )
            } else {
                AppRule(packageName = packageName, mode = BlockMode.FULL_BLOCK)
            }
            rulesRepository.upsert(rule)
        }
    }

    fun updateRule(rule: AppRule) {
        viewModelScope.launch {
            rulesRepository.upsert(rule)
        }
    }

    fun removeRule(packageName: String) {
        viewModelScope.launch {
            rulesRepository.remove(packageName)
        }
    }

    @Suppress("DEPRECATION", "QueryPermissionsNeeded")
    private fun queryLaunchableApps(): List<InstalledApp> {
        val packageManager = getApplication<Application>().packageManager
        val ownPackage = getApplication<Application>().packageName
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { resolveInfo ->
                val activityPackage = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (activityPackage == ownPackage) {
                    return@mapNotNull null
                }
                val icon = runCatching {
                    resolveInfo.loadIcon(packageManager)
                        .toBitmap(width = APP_ICON_SIZE_PX, height = APP_ICON_SIZE_PX)
                        .asImageBitmap()
                }.getOrNull()
                InstalledApp(
                    packageName = activityPackage,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = icon,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
