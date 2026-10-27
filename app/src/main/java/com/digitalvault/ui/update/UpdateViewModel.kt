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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class UpdateStatus { IDLE, DOWNLOADING, FAILED }

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set

    var status by mutableStateOf(UpdateStatus.IDLE)
        private set

    var isDownloaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val info = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
            val downloadedApk = withContext(Dispatchers.IO) {
                val apk = info?.let { UpdateInstaller.findDownloadedApk(context, it) }
                UpdateInstaller.deleteDownloadsExcept(context, keep = apk)
                apk
            }
            isDownloaded = downloadedApk != null
            updateInfo = info
        }
    }

    fun dismiss() {
        updateInfo = null
    }

    fun downloadAndInstall() {
        val info = updateInfo ?: return
        val context = getApplication<Application>()
        viewModelScope.launch {
            val downloadedApk = withContext(Dispatchers.IO) { UpdateInstaller.findDownloadedApk(context, info) }
            if (downloadedApk == null) {
                status = UpdateStatus.DOWNLOADING
            }
            val file = downloadedApk ?: UpdateInstaller.downloadApk(context, info)
            if (file == null) {
                status = UpdateStatus.FAILED

                return@launch
            }
            status = UpdateStatus.IDLE
            isDownloaded = true
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
