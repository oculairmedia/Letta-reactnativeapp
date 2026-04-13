package com.letta.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.letta.mobile.bot.service.BotServiceAutoStarter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BotRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (!shouldRestoreBotFromBroadcast(action)) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BotRestoreReceiverEntryPoint::class.java,
                )
                entryPoint.botServiceAutoStarter().restoreIfConfigured()
            } catch (error: Exception) {
                Log.w(TAG, "Skipping bot restore for broadcast $action", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BotRestoreReceiver"
    }
}

internal fun shouldRestoreBotFromBroadcast(action: String?): Boolean =
    action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BotRestoreReceiverEntryPoint {
    fun botServiceAutoStarter(): BotServiceAutoStarter
}
