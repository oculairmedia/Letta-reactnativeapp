package com.letta.mobile.di

import com.letta.mobile.data.repository.BlockRepository
import com.letta.mobile.data.repository.api.IBlockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindBlockRepository(impl: BlockRepository): IBlockRepository
}
