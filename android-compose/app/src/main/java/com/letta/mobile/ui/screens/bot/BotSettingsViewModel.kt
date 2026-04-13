package com.letta.mobile.ui.screens.bot

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.bot.config.BotConfig
import com.letta.mobile.bot.config.BotConfigStore
import com.letta.mobile.bot.service.BotForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BotSettingsViewModel @Inject constructor(
    application: Application,
    private val configStore: BotConfigStore,
) : AndroidViewModel(application) {

    val configs: StateFlow<List<BotConfig>> = configStore.configs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isBotRunning: StateFlow<Boolean> = configStore.configs
        .map { isBotServiceRunning() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), isBotServiceRunning())

    fun startBot() {
        val context = getApplication<Application>()
        context.startForegroundService(BotForegroundService.startIntent(context))
    }

    fun stopBot() {
        val context = getApplication<Application>()
        context.startService(BotForegroundService.stopIntent(context))
    }

    fun deleteConfig(configId: String) {
        viewModelScope.launch { configStore.deleteConfig(configId) }
    }

    fun toggleConfigEnabled(config: BotConfig) {
        viewModelScope.launch {
            configStore.saveConfig(config.copy(enabled = !config.enabled))
        }
    }

    @Suppress("DEPRECATION")
    private fun isBotServiceRunning(): Boolean {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == BotForegroundService::class.java.name }
    }
}
