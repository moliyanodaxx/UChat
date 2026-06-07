package com.example.uchat.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "uchat_prefs")

class PrefsManager(private val context: Context) {

    companion object {
        val KEY_ACCOUNT_ID = stringPreferencesKey("account_id")
        val KEY_PASSWORD = stringPreferencesKey("password")
        val KEY_NICKNAME = stringPreferencesKey("nickname")
        val KEY_AVATAR = stringPreferencesKey("avatar")
        val KEY_SIGNATURE = stringPreferencesKey("signature")
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
        val KEY_AUTO_LOGIN = booleanPreferencesKey("auto_login")
    }

    val accountId: Flow<String> = context.dataStore.data.map { it[KEY_ACCOUNT_ID] ?: "" }
    val password: Flow<String> = context.dataStore.data.map { it[KEY_PASSWORD] ?: "" }
    val nickname: Flow<String> = context.dataStore.data.map { it[KEY_NICKNAME] ?: "" }
    val avatar: Flow<String> = context.dataStore.data.map { it[KEY_AVATAR] ?: "" }
    val signature: Flow<String> = context.dataStore.data.map { it[KEY_SIGNATURE] ?: "" }
    val serverUrl: Flow<String> = context.dataStore.data.map { it[KEY_SERVER_URL] ?: "" }
    val autoLogin: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_LOGIN] ?: false }

    suspend fun saveCredentials(accountId: String, password: String) {
        context.dataStore.edit {
            it[KEY_ACCOUNT_ID] = accountId
            it[KEY_PASSWORD] = password
            it[KEY_AUTO_LOGIN] = true
        }
    }

    suspend fun saveProfile(nickname: String, avatar: String, signature: String = "") {
        context.dataStore.edit {
            it[KEY_NICKNAME] = nickname
            it[KEY_AVATAR] = avatar
            it[KEY_SIGNATURE] = signature
        }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit {
            it.remove(KEY_ACCOUNT_ID)
            it.remove(KEY_PASSWORD)
            it[KEY_AUTO_LOGIN] = false
        }
    }
}
