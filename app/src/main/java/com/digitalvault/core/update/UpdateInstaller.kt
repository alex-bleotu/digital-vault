package com.digitalvault.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.digitalvault.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val UPDATE_TAG = "VaultUpdate"
private const val TIMEOUT_MILLIS = 15_000
private const val MAX_REDIRECTS = 5

object UpdateInstaller {

    suspend fun downloadApk(context: Context, apkAssetApiUrl: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(updatesDir, "digital-vault-update.apk")

            var currentUrl = apkAssetApiUrl
            var sendAuth = true

            for (redirectCount in 0 until MAX_REDIRECTS) {
                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/octet-stream")
                if (sendAuth && BuildConfig.UPDATE_CHECK_TOKEN.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.UPDATE_CHECK_TOKEN}")
                }
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS

                val code = connection.responseCode
                if (code in intArrayOf(
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_MOVED_PERM,
                        307,
                        308,
                    )
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location == null) {
                        return@withContext null
                    }
                    currentUrl = location
                    sendAuth = false

                    continue
                }

                if (code == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    connection.disconnect()

                    return@withContext outFile
                }

                connection.disconnect()

                return@withContext null
            }

            null
        }.onFailure {
            Log.w(UPDATE_TAG, "Update download failed", it)
        }.getOrNull()
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
}
