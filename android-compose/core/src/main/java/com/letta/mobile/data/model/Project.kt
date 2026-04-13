package com.letta.mobile.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ProjectSummary(
    val identifier: String,
    val name: String,
    @SerialName("filesystem_path") val filesystemPath: String? = null,
    @SerialName("git_url") val gitUrl: String? = null,
    val status: String? = null,
    @SerialName("vibe_id") val vibeId: String? = null,
    @SerialName("huly_id") val hulyId: String? = null,
    @SerialName("letta_agent_id") val lettaAgentId: String? = null,
    @SerialName("letta_folder_id") val lettaFolderId: String? = null,
    @SerialName("letta_source_id") val lettaSourceId: String? = null,
    @SerialName("issue_count") val issueCount: Int? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("tech_stack") val techStack: String? = null,
    @SerialName("beads_issue_count") val beadsIssueCount: Int? = null,
    @SerialName("beads_prefix") val beadsPrefix: String? = null,
    val description: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("last_scan_at") val lastScanAt: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("last_checked_at") val lastCheckedAt: String? = null,
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("mcp_enabled") val mcpEnabled: Boolean? = null,
)

@Serializable
data class ProjectCatalog(
    val total: Int,
    val projects: List<ProjectSummary>,
    val timestamp: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive || element.content == "null") return null
        return element.longOrNull?.toString() ?: element.content
    }
}

@OptIn(ExperimentalSerializationApi::class)
object FlexibleBooleanSerializer : KSerializer<Boolean?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: Boolean?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeBoolean(value)
        }
    }

    override fun deserialize(decoder: Decoder): Boolean? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive || element.content == "null") return null
        return element.booleanOrNull
            ?: element.intOrNull?.let { it != 0 }
            ?: when (element.content.lowercase()) {
                "true", "yes", "on" -> true
                "false", "no", "off" -> false
                else -> null
            }
    }
}
