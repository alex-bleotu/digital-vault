package com.digitalvault.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TIMEOUT_MILLIS = 15_000
private const val UPDATES_DIRECTORY_NAME = "updates"
private const val PARTIAL_DOWNLOAD_SUFFIX = ".part"

object UpdateInstaller {

    suspend fun downloadApk(context: Context, info: UpdateInfo): File? = withContext(Dispatchers.IO) {
        runCatching {
            val apkFile = apkFileFor(context, info)
            deleteDownloadsExcept(context, keep = null)
            apkFile.parentFile?.mkdirs()
            val partialFile = File(apkFile.parentFile, apkFile.name + PARTIAL_DOWNLOAD_SUFFIX)

            val connection = URL(info.apkDownloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()

                return@withContext null
            }

            connection.inputStream.use { input ->
                partialFile.outputStream().use { output -> input.copyTo(output) }
            }
            connection.disconnect()

            if (!isCompleteDownload(partialFile, info) || !partialFile.renameTo(apkFile)) {
                partialFile.delete()

                return@withContext null
            }

            apkFile
        }.getOrNull()
    }

    fun findDownloadedApk(context: Context, info: UpdateInfo): File? =
        apkFileFor(context, info).takeIf { it.isFile && isCompleteDownload(it, info) }

    fun deleteDownloadsExcept(context: Context, keep: File?) {
        updatesDirectory(context).listFiles()?.forEach { file ->
            if (file != keep) {
                file.deleteRecursively()
            }
        }
    }

    fun canInstallPackages(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun updatesDirectory(context: Context): File = File(context.cacheDir, UPDATES_DIRECTORY_NAME)

    private fun apkFileFor(context: Context, info: UpdateInfo): File =
        File(updatesDirectory(context), "digital-vault-${info.versionCode}.apk")

    private fun isCompleteDownload(file: File, info: UpdateInfo): Boolean =
        file.length() > 0 && (info.apkSizeBytes <= 0 || file.length() == info.apkSizeBytes)
}
