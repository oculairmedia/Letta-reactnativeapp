package com.letta.mobile.ui.screens.settings

import com.letta.mobile.bot.protocol.ExternalBotClient
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.withTimeout

@ViewModelScoped
class ClientModeConnectionTester @Inject constructor() {
    suspend fun test(baseUrl: String, apiKey: String?): Result<Unit> = runCatching {
        withTimeout(10_000) {
            ExternalBotClient(baseUrl = baseUrl, token = apiKey?.takeIf { it.isNotBlank() }).use { client ->
                client.getStatus()
            }
        }
    }.map { Unit }
}
