package com.letta.mobile.bot.config

import kotlinx.coroutines.flow.Flow

interface IBotServerProfileStore {
    val profiles: Flow<List<BotServerProfile>>

    suspend fun saveProfile(profile: BotServerProfile)

    suspend fun deleteProfile(profileId: String)

    suspend fun getAll(): List<BotServerProfile>

    suspend fun activateProfile(profileId: String)

    suspend fun findById(profileId: String): BotServerProfile?

    suspend fun getActiveProfile(): BotServerProfile?
}
