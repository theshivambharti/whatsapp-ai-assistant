package com.shivam.whatsappai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "whatsapp_ai_settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val HEADER_NAME_KEY = stringPreferencesKey("header_name")
        val HEADER_VALUE_KEY = stringPreferencesKey("header_value")
        val SERVICE_ACTIVE_KEY = booleanPreferencesKey("service_active")
        val TEST_MODE_KEY = booleanPreferencesKey("test_mode")
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val url = preferences[SERVER_URL_KEY]
            if (url.isNullOrBlank()) "https://bot.clickbaaz.com/webhook.php" else url
        }

    val headerNameFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[HEADER_NAME_KEY] ?: ""
        }

    val headerValueFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[HEADER_VALUE_KEY] ?: ""
        }

    val serviceActiveFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[SERVICE_ACTIVE_KEY] ?: true
        }

    val testModeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[TEST_MODE_KEY] ?: false
        }

    suspend fun saveSettings(serverUrl: String, headerName: String, headerValue: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = serverUrl
            preferences[HEADER_NAME_KEY] = headerName
            preferences[HEADER_VALUE_KEY] = headerValue
        }
    }

    suspend fun setServiceActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_ACTIVE_KEY] = active
        }
    }

    suspend fun setTestMode(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TEST_MODE_KEY] = active
        }
    }
}
