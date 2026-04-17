package com.letta.poc

import java.time.Instant
import java.util.UUID

/**
 * Single unified timeline event model.
 *
 * Matrix-inspired: each event has a client-generated `otid` (txn_id analog).
 * Local events represent optimistic sends. Confirmed events come from the server.
 * Both coexist in the timeline; on server echo we swap Local→Confirmed in place.
 */
sealed class TimelineEvent {
    abstract val position: Double      // monotonic ordering key
    abstract val otid: String          // client- or server-generated
    abstract val content: String

    /** Optimistic, not yet confirmed by server. */
    data class Local(
        override val position: Double,
        override val otid: String,
        override val content: String,
        val role: Role = Role.USER,
        val sentAt: Instant,
        val deliveryState: DeliveryState,
    ) : TimelineEvent()

    /** Confirmed via server GET or stream. */
    data class Confirmed(
        override val position: Double,
        override val otid: String,
        override val content: String,
        val serverId: String,
        val messageType: MessageType,
        val date: Instant,
        val runId: String?,
        val stepId: String?,
    ) : TimelineEvent()
}

enum class Role { USER, ASSISTANT, SYSTEM }

enum class DeliveryState { SENDING, SENT, FAILED }

enum class MessageType {
    USER, ASSISTANT, REASONING, TOOL_CALL, TOOL_RETURN, SYSTEM, OTHER
}

/**
 * Per-conversation timeline. Events are always ordered by position.
 *
 * Invariants:
 * - positions are strictly increasing
 * - otids are unique within the timeline
 * - Local events can only be replaced by Confirmed events with matching otid
 */
data class Timeline(
    val conversationId: String,
    val events: List<TimelineEvent> = emptyList(),
    val liveCursor: String? = null,      // last known server message id
    val backfillCursor: String? = null,  // for pagination of older messages
) {
    init {
        require(events.zipWithNext().all { (a, b) -> a.position < b.position }) {
            "Timeline events must be strictly ordered by position"
        }
        require(events.map { it.otid }.toSet().size == events.size) {
            "otids must be unique within a timeline"
        }
    }

    fun nextLocalPosition(): Double {
        val last = events.lastOrNull()?.position ?: 0.0
        // Local events get fractional positions so they slot between server events
        return last + 1.0
    }

    fun findByOtid(otid: String): TimelineEvent? = events.firstOrNull { it.otid == otid }

    /**
     * Append a new event. Must have position strictly greater than the current last.
     */
    fun append(event: TimelineEvent): Timeline {
        require(events.lastOrNull()?.let { it.position < event.position } ?: true) {
            "Appended event position ${event.position} must be > last ${events.lastOrNull()?.position}"
        }
        require(findByOtid(event.otid) == null) {
            "otid ${event.otid} already in timeline"
        }
        return copy(events = events + event)
    }

    /**
     * Replace a Local event (identified by otid) with a Confirmed event.
     * Preserves the Local's position to avoid visual jumps.
     */
    fun replaceLocal(otid: String, confirmed: TimelineEvent.Confirmed): Timeline {
        val idx = events.indexOfFirst { it.otid == otid && it is TimelineEvent.Local }
        if (idx == -1) {
            // Not a replacement — could be a fresh confirmed event from server
            return insertOrdered(confirmed)
        }
        val local = events[idx]
        // Preserve the Local's position so it doesn't jump
        val stabilized = confirmed.copy(position = local.position)
        val newEvents = events.toMutableList().also { it[idx] = stabilized }
        return copy(events = newEvents)
    }

    /**
     * Insert a Confirmed event at its correct ordered position.
     * Used for backfill (older messages loaded via pagination).
     */
    fun insertOrdered(event: TimelineEvent): Timeline {
        if (findByOtid(event.otid) != null) return this  // dedupe
        val insertIdx = events.indexOfFirst { it.position > event.position }
        val newEvents = if (insertIdx == -1) events + event
                       else events.toMutableList().also { it.add(insertIdx, event) }
        return copy(events = newEvents)
    }

    /** Mark a Local event as SENT (POST succeeded). */
    fun markSent(otid: String): Timeline {
        val idx = events.indexOfFirst { it.otid == otid && it is TimelineEvent.Local }
        if (idx == -1) return this
        val local = events[idx] as TimelineEvent.Local
        val updated = local.copy(deliveryState = DeliveryState.SENT)
        return copy(events = events.toMutableList().also { it[idx] = updated })
    }

    /** Mark a Local event as FAILED. */
    fun markFailed(otid: String): Timeline {
        val idx = events.indexOfFirst { it.otid == otid && it is TimelineEvent.Local }
        if (idx == -1) return this
        val local = events[idx] as TimelineEvent.Local
        val updated = local.copy(deliveryState = DeliveryState.FAILED)
        return copy(events = events.toMutableList().also { it[idx] = updated })
    }
}

/** Generate a new client-side otid for outgoing messages. */
fun newOtid(): String = "client-${UUID.randomUUID()}"
