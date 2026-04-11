package com.letta.mobile.channel

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class ChannelHeartbeatWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ChannelHeartbeatWorkerEntryPoint::class.java,
        )
        return entryPoint.channelHeartbeatSync().run()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChannelHeartbeatWorkerEntryPoint {
    fun channelHeartbeatSync(): ChannelHeartbeatSync
}
