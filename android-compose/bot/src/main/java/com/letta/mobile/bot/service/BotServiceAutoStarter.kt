package com.letta.mobile.bot.service

import android.content.Context
import com.letta.mobile.bot.config.BotConfig
import com.letta.mobile.bot.config.BotConfigStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BotServiceAutoStarter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configStore: BotConfigStore,
) {
    suspend fun restoreIfConfigured(): Boolean = withContext(Dispatchers.IO) {
        val shouldRestore = shouldAutoStartBotService(configStore.getAll())
        if (shouldRestore) {
            context.startForegroundService(BotForegroundService.startIntent(context))
        }
        shouldRestore
    }
}

internal fun shouldAutoStartBotService(configs: List<BotConfig>): Boolean =
    configs.any { it.enabled && it.autoStart }
