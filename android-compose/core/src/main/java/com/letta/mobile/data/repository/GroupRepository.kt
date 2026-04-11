package com.letta.mobile.data.repository

import com.letta.mobile.data.api.GroupApi
import com.letta.mobile.data.model.Group
import com.letta.mobile.data.model.GroupCreateParams
import com.letta.mobile.data.model.GroupUpdateParams
import com.letta.mobile.data.model.LettaMessage
import com.letta.mobile.data.model.LettaResponse
import com.letta.mobile.data.model.MessageCreateRequest
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupApi: GroupApi,
) {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    suspend fun refreshGroups(managerType: String? = null, projectId: String? = null, showHiddenGroups: Boolean? = null) {
        _groups.value = groupApi.listGroups(limit = 1000, managerType = managerType, projectId = projectId, showHiddenGroups = showHiddenGroups)
    }

    suspend fun countGroups(): Int = groupApi.countGroups()

    suspend fun getGroup(groupId: String): Group {
        return groupApi.retrieveGroup(groupId)
    }

    suspend fun createGroup(params: GroupCreateParams): Group {
        val group = groupApi.createGroup(params)
        upsertGroup(group)
        return group
    }

    suspend fun updateGroup(groupId: String, params: GroupUpdateParams): Group {
        val group = groupApi.updateGroup(groupId, params)
        upsertGroup(group)
        return group
    }

    suspend fun deleteGroup(groupId: String) {
        groupApi.deleteGroup(groupId)
        _groups.update { current -> current.filterNot { it.id == groupId } }
    }

    suspend fun sendGroupMessage(groupId: String, request: MessageCreateRequest): LettaResponse {
        return groupApi.sendGroupMessage(groupId, request)
    }

    suspend fun sendGroupMessageStream(groupId: String, request: MessageCreateRequest): ByteReadChannel {
        return groupApi.sendGroupMessageStream(groupId, request)
    }

    suspend fun updateGroupMessage(groupId: String, messageId: String, request: JsonElement): LettaMessage {
        return groupApi.updateGroupMessage(groupId, messageId, request)
    }

    suspend fun listGroupMessages(groupId: String): List<LettaMessage> {
        return groupApi.listGroupMessages(groupId = groupId, limit = 1000)
    }

    suspend fun resetGroupMessages(groupId: String) {
        groupApi.resetGroupMessages(groupId)
    }

    private fun upsertGroup(group: Group) {
        _groups.update { current ->
            val index = current.indexOfFirst { it.id == group.id }
            if (index >= 0) {
                current.toMutableList().apply { this[index] = group }
            } else {
                current + group
            }
        }
    }
}
