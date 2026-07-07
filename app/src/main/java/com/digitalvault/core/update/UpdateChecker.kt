package com.digitalvault.core.update

import android.util.Log
import com.digitalvault.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val UPDATE_TAG = "VaultUpdate"
private const val TIMEOUT_MILLIS = 10_000

object UpdateChecker {

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(
                "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest",
            ).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (BuildConfig.UPDATE_CHECK_TOKEN.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.UPDATE_CHECK_TOKEN}")
            }
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()

                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            val tagName = json.optString("tag_name")
            val versionCode = tagName.removePrefix("v").toIntOrNull() ?: return@withContext null
            if (versionCode <= currentVersionCode) {
                return@withContext null
            }

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkAssetApiUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.optString("name").endsWith(".apk")) {
                    apkAssetApiUrl = asset.optString("url")
                    break
                }
            }

            val assetUrl = apkAssetApiUrl ?: return@withContext null

            UpdateInfo(versionCode = versionCode, versionTag = tagName, apkAssetApiUrl = assetUrl)
        }.onFailure {
            Log.w(UPDATE_TAG, "Update check failed", it)
        }.getOrNull()
    }
}
