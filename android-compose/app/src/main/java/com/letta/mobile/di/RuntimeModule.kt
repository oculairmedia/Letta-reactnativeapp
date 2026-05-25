package com.letta.mobile.di

import com.letta.mobile.data.local.LettaDatabase
import com.letta.mobile.data.local.RoomMemFsStore
import com.letta.mobile.data.local.RoomRuntimeEventOutbox
import com.letta.mobile.runtime.MemFsStore
import com.letta.mobile.runtime.RuntimeEventOutbox
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RuntimeModule {
    @Provides
    @Singleton
    fun provideRuntimeEventOutbox(database: LettaDatabase): RuntimeEventOutbox =
        RoomRuntimeEventOutbox(database)

    @Provides
    @Singleton
    fun provideMemFsStore(database: LettaDatabase): MemFsStore =
        RoomMemFsStore(database)
}
