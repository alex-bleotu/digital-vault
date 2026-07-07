package com.digitalvault.ui.update

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.BuildConfig
import com.digitalvault.core.update.UpdateChecker
import com.digitalvault.core.update.UpdateInfo
import com.digitalvault.core.update.UpdateInstaller
import kotlinx.coroutines.launch

enum class UpdateStatus { IDLE, DOWNLOADING, FAILED }

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set

    var status by mutableStateOf(UpdateStatus.IDLE)
        private set

    init {
        viewModelScope.launch {
            updateInfo = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
        }
    }

    fun dismiss() {
        updateInfo = null
    }

    fun downloadAndInstall() {
        val info = updateInfo ?: return
        val context = getApplication<Application>()
        viewModelScope.launch {
            status = UpdateStatus.DOWNLOADING
            val file = UpdateInstaller.downloadApk(context, info.apkAssetApiUrl)
            if (file == null) {
                status = UpdateStatus.FAILED

                return@launch
            }
            status = UpdateStatus.IDLE
            if (UpdateInstaller.canInstallPackages(context)) {
                UpdateInstaller.installApk(context, file)
            } else {
                context.startActivity(
                    UpdateInstaller.unknownSourcesSettingsIntent(context)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
