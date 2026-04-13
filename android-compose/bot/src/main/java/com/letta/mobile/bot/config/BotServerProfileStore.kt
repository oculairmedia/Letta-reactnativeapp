package com.letta.mobile.bot.config

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.botServerProfileDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "bot_server_profiles")

@Singleton
class BotServerProfileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : IBotServerProfileStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val profilesKey = stringPreferencesKey("bot_server_profiles_json")

    override val profiles: Flow<List<BotServerProfile>> = context.botServerProfileDataStore.data.map { prefs ->
        parseProfiles(prefs[profilesKey])
    }

    override suspend fun saveProfile(profile: BotServerProfile) {
        context.botServerProfileDataStore.edit { prefs ->
            val current = parseProfiles(prefs[profilesKey])
            val updated = current.filterNot { it.id == profile.id } + profile
            prefs[profilesKey] = json.encodeToString(updated)
        }
    }

    override suspend fun deleteProfile(profileId: String) {
        context.botServerProfileDataStore.edit { prefs ->
            val current = parseProfiles(prefs[profilesKey])
            prefs[profilesKey] = json.encodeToString(current.filterNot { it.id == profileId })
        }
    }

    override suspend fun getAll(): List<BotServerProfile> {
        var result = emptyList<BotServerProfile>()
        context.botServerProfileDataStore.edit { prefs ->
            result = parseProfiles(prefs[profilesKey])
        }
        return result
    }

    override suspend fun activateProfile(profileId: String) {
        context.botServerProfileDataStore.edit { prefs ->
            val current = parseProfiles(prefs[profilesKey])
            val updated = current.map { it.copy(isActive = it.id == profileId) }
            prefs[profilesKey] = json.encodeToString(updated)
        }
    }

    override suspend fun findById(profileId: String): BotServerProfile? = getAll().firstOrNull { it.id == profileId }

    override suspend fun getActiveProfile(): BotServerProfile? = getAll().firstOrNull { it.isActive }

    private fun parseProfiles(raw: String?): List<BotServerProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse bot server profiles", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "BotServerProfileStore"
    }
}
