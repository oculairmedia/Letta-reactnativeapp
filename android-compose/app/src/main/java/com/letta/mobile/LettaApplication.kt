package com.letta.mobile

import android.app.Application
import android.util.Log
import com.letta.mobile.channel.ChannelHeartbeatScheduler
import com.letta.mobile.channel.ChannelNotificationPublisher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LettaApplication : Application() {
    @Inject
    lateinit var channelHeartbeatScheduler: ChannelHeartbeatScheduler

    @Inject
    lateinit var channelNotificationPublisher: ChannelNotificationPublisher

    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
        channelNotificationPublisher.ensureChannel()
        if (isRobolectricRuntime()) {
            return
        }
        runCatching {
            channelHeartbeatScheduler.schedule()
        }.onFailure { error ->
            Log.w("LettaApp", "Skipping heartbeat scheduling", error)
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LettaApp", "Uncaught exception on ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun isRobolectricRuntime(): Boolean {
        return runCatching {
            Class.forName("org.robolectric.RuntimeEnvironment")
        }.isSuccess
    }
}
