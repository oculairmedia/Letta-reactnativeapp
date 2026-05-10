package com.letta.mobile.bot.config

import android.content.Context
import com.letta.mobile.util.EncryptedPrefsHelper

internal interface BotSecureTokenStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

internal class SharedPreferencesBotSecureTokenStore(context: Context) : BotSecureTokenStore {
    private val prefs = EncryptedPrefsHelper.getEncryptedPrefs(context)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

internal data class BotTokenMigrationResult<T>(
    val values: List<T>,
    val foundPlaintextTokens: Boolean,
)

internal fun hydrateAndStoreBotConfigTokens(
    configs: List<BotConfig>,
    tokenStore: BotSecureTokenStore,
): BotTokenMigrationResult<BotConfig> {
    var foundPlaintextTokens = false
    val hydrated = configs.map { config ->
        val remoteKey = BotTokenKeys.configRemoteToken(config.id)
        val apiServerKey = BotTokenKeys.configApiServerToken(config.id)

        if (config.remoteToken != null) {
            foundPlaintextTokens = foundPlaintextTokens || config.remoteToken.isNotBlank()
            if (config.remoteToken.isBlank()) tokenStore.remove(remoteKey) else tokenStore.putString(remoteKey, config.remoteToken)
        }
        if (config.apiServerToken != null) {
            foundPlaintextTokens = foundPlaintextTokens || config.apiServerToken.isNotBlank()
            if (config.apiServerToken.isBlank()) tokenStore.remove(apiServerKey) else tokenStore.putString(apiServerKey, config.apiServerToken)
        }

        config.copy(
            remoteToken = tokenStore.getString(remoteKey),
            apiServerToken = tokenStore.getString(apiServerKey),
        )
    }
    return BotTokenMigrationResult(hydrated, foundPlaintextTokens)
}

internal fun sanitizeBotConfigTokens(config: BotConfig): BotConfig = config.copy(
    remoteToken = null,
    apiServerToken = null,
)

internal fun hydrateAndStoreBotServerProfileTokens(
    profiles: List<BotServerProfile>,
    tokenStore: BotSecureTokenStore,
): BotTokenMigrationResult<BotServerProfile> {
    var foundPlaintextTokens = false
    val hydrated = profiles.map { profile ->
        val key = BotTokenKeys.serverProfileAuthToken(profile.id)
        if (profile.authToken != null) {
            foundPlaintextTokens = foundPlaintextTokens || profile.authToken.isNotBlank()
            if (profile.authToken.isBlank()) tokenStore.remove(key) else tokenStore.putString(key, profile.authToken)
        }
        profile.copy(authToken = tokenStore.getString(key))
    }
    return BotTokenMigrationResult(hydrated, foundPlaintextTokens)
}

internal fun sanitizeBotServerProfileToken(profile: BotServerProfile): BotServerProfile = profile.copy(
    authToken = null,
)

internal object BotTokenKeys {
    fun configRemoteToken(configId: String): String = "bot_config.$configId.remote_token"
    fun configApiServerToken(configId: String): String = "bot_config.$configId.api_server_token"
    fun serverProfileAuthToken(profileId: String): String = "bot_server_profile.$profileId.auth_token"
}
