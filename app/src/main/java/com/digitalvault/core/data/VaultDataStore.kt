package com.digitalvault.core.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.createMultiProcessCoordinator
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source

private const val DATA_STORE_NAME = "digital_vault"
private const val LOG_TAG = "VaultDataStore"

@Volatile
private var instance: DataStore<Preferences>? = null
private val lock = Any()

val Context.vaultDataStore: DataStore<Preferences>
    get() = instance ?: synchronized(lock) {
        instance ?: run {
            val dataFile = File(applicationContext.filesDir, "datastore/$DATA_STORE_NAME.preferences_pb").absoluteFile
            val backupFile = File(dataFile.parentFile, "${dataFile.name}.bak")
            val storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = BackedUpPreferencesSerializer(backupFile),
                coordinatorProducer = { path, _ -> createMultiProcessCoordinator(Dispatchers.IO, path.toFile()) },
                producePath = { dataFile.toOkioPath() },
            )
            MultiProcessDataStoreFactory.create(
                storage = storage,
                corruptionHandler = ReplaceFileCorruptionHandler { exception ->
                    recoverFromBackup(dataFile, backupFile, exception)
                },
            ).also { instance = it }
        }
    }

private class BackedUpPreferencesSerializer(private val backupFile: File) : OkioSerializer<Preferences> {
    override val defaultValue: Preferences = PreferencesSerializer.defaultValue

    override suspend fun readFrom(source: BufferedSource): Preferences = PreferencesSerializer.readFrom(source)

    override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
        val buffer = Buffer()
        PreferencesSerializer.writeTo(t, buffer)
        val bytes = buffer.readByteArray()
        sink.write(bytes)
        writeBackup(bytes)
    }

    private fun writeBackup(bytes: ByteArray) {
        val scratchFile = File(backupFile.parentFile, "${backupFile.name}.tmp")
        try {
            FileOutputStream(scratchFile).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            if (!scratchFile.renameTo(backupFile)) {
                Log.e(LOG_TAG, "Could not move preferences backup into place at ${backupFile.path}")
            }
        } catch (exception: IOException) {
            Log.e(LOG_TAG, "Could not write preferences backup to ${backupFile.path}", exception)
        }
    }
}

private fun recoverFromBackup(dataFile: File, backupFile: File, exception: CorruptionException): Preferences {
    Log.e(LOG_TAG, "Preferences file ${dataFile.path} (${dataFile.length()} bytes) is corrupt", exception)
    if (!backupFile.exists()) {
        Log.e(LOG_TAG, "No preferences backup at ${backupFile.path}, starting from empty preferences")
        return emptyPreferences()
    }
    return try {
        backupFile.source().buffer().use { source -> runBlocking { PreferencesSerializer.readFrom(source) } }
            .also { Log.w(LOG_TAG, "Restored ${it.asMap().size} preferences from ${backupFile.path}") }
    } catch (backupException: CorruptionException) {
        Log.e(LOG_TAG, "Preferences backup ${backupFile.path} is also corrupt, starting from empty preferences", backupException)
        emptyPreferences()
    } catch (backupException: IOException) {
        Log.e(LOG_TAG, "Could not read preferences backup ${backupFile.path}, starting from empty preferences", backupException)
        emptyPreferences()
    }
}
