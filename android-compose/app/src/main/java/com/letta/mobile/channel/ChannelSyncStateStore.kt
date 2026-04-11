package com.letta.mobile.channel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelSyncStateStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getProcessedLastActivityAt(conversationId: String): String? {
        return prefs.getString(processedKey(conversationId), null)
    }

    fun setProcessedLastActivityAt(conversationId: String, value: String) {
        prefs.edit().putString(processedKey(conversationId), value).apply()
    }

    fun getLastNotifiedMessageId(conversationId: String): String? {
        return prefs.getString(notifiedKey(conversationId), null)
    }

    fun setLastNotifiedMessageId(conversationId: String, messageId: String) {
        prefs.edit().putString(notifiedKey(conversationId), messageId).apply()
    }

    private fun processedKey(conversationId: String) = "processed::$conversationId"

    private fun notifiedKey(conversationId: String) = "notified::$conversationId"

    companion object {
        private const val PREFS_NAME = "channel_sync_state"
    }
}
