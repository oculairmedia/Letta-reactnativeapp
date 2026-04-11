package com.letta.mobile.data.repository

import com.letta.mobile.data.api.ScheduleApi
import com.letta.mobile.data.model.ScheduleCreateParams
import com.letta.mobile.data.model.ScheduledMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleApi: ScheduleApi,
) {
    private val _schedules = MutableStateFlow<Map<String, List<ScheduledMessage>>>(emptyMap())

    fun getSchedules(agentId: String): Flow<List<ScheduledMessage>> {
        return _schedules.asStateFlow().map { current -> current[agentId] ?: emptyList() }
    }

    suspend fun refreshSchedules(agentId: String, limit: Int? = 100, after: String? = null) {
        val response = scheduleApi.listSchedules(agentId = agentId, limit = limit, after = after)
        _schedules.update { current ->
            current.toMutableMap().apply { put(agentId, response.scheduledMessages) }
        }
    }

    suspend fun getSchedule(agentId: String, scheduledMessageId: String): ScheduledMessage {
        return scheduleApi.retrieveSchedule(agentId, scheduledMessageId)
    }

    suspend fun createSchedule(agentId: String, params: ScheduleCreateParams): ScheduledMessage {
        val schedule = scheduleApi.createSchedule(agentId, params)
        refreshSchedules(agentId)
        return schedule
    }

    suspend fun deleteSchedule(agentId: String, scheduledMessageId: String) {
        scheduleApi.deleteSchedule(agentId, scheduledMessageId)
        _schedules.update { current ->
            current.toMutableMap().apply {
                val existing = get(agentId) ?: emptyList()
                put(agentId, existing.filterNot { it.id == scheduledMessageId })
            }
        }
    }
}
