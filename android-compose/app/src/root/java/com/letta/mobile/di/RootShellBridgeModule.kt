package com.letta.mobile.di

import com.letta.mobile.platform.root.RootShellBridge
import com.letta.mobile.platform.root.SuRootShellBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RootShellBridgeModule {
    @Binds
    @Singleton
    abstract fun bindRootShellBridge(impl: SuRootShellBridge): RootShellBridge
}
