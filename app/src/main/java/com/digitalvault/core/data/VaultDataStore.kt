package com.digitalvault.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.createMultiProcessCoordinator
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path.Companion.toOkioPath

private const val DATA_STORE_NAME = "digital_vault"

@Volatile
private var instance: DataStore<Preferences>? = null
private val lock = Any()

val Context.vaultDataStore: DataStore<Preferences>
    get() = instance ?: synchronized(lock) {
        instance ?: run {
            val storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                coordinatorProducer = { path, _ -> createMultiProcessCoordinator(Dispatchers.IO, path.toFile()) },
                producePath = {
                    File(applicationContext.filesDir, "datastore/$DATA_STORE_NAME.preferences_pb")
                        .absoluteFile
                        .toOkioPath()
                },
            )
            MultiProcessDataStoreFactory.create(storage = storage).also { instance = it }
        }
    }
