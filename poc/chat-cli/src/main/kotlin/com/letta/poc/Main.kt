package com.letta.poc

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.system.exitProcess

private const val DEFAULT_BASE_URL = "http://192.168.50.90:8289"
private const val DEFAULT_AGENT = "agent-d53a5c94-908d-4b6d-a95c-cce0466cf1c3"  // Letta Mobile Admin
private const val DEFAULT_TOKEN = "sk-let-ODZjNWE5NzQtNTlkYi00YWQxLWEwOTItYWI1ZDE1NjUxNDcwOjYyMWIxNmIxLWJjNmUtNDIwZC1hYmE3LWQ1MWFkMTY4MjM4Yg=="

fun main(args: Array<String>): Unit = runBlocking {
    val baseUrl = System.getenv("LETTA_BASE_URL") ?: DEFAULT_BASE_URL
    val token = System.getenv("LETTA_TOKEN") ?: DEFAULT_TOKEN
    val agentId = args.getOrNull(0) ?: DEFAULT_AGENT

    val api = LettaApi(baseUrl, token)
    println("🔌 Letta Timeline POC CLI")
    println("   baseUrl: $baseUrl")
    println("   agent:   $agentId")

    // Create or reuse a conversation
    val conversationId = if (args.size >= 2) args[1] else {
        val id = api.createConversation(agentId, name = "poc-${System.currentTimeMillis()}")
        println("   created conversation: $id")
        id
    }
    println("   conv:    $conversationId")
    println()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val sync = SyncLoop(api, conversationId, scope)

    // Subscribe to events
    val eventJob = scope.launch {
        sync.events.collect { event ->
            when (event) {
                is SyncEvent.Hydrated -> println("  [sync] hydrated ${event.messageCount} messages")
                is SyncEvent.LocalAppended -> println("  [sync] local appended (otid=${event.otid.take(20)}...)")
                is SyncEvent.LocalConfirmed -> println("  [sync] ✓ local confirmed (serverId=${event.serverId})")
                is SyncEvent.ServerEvent -> {
                    val m = event.message
                    println("  [sync] server event: ${m.messageType} (otid=${m.otid?.takeLast(8)}) \"${m.content.take(60)}\"")
                }
                is SyncEvent.StopReason -> println("  [sync] stop: ${event.reason}")
                is SyncEvent.StreamError -> println("  [sync] ⚠ error: ${event.type} — ${event.message}")
                is SyncEvent.ReconcileError -> println("  [sync] ⚠ reconcile error: ${event.message}")
            }
        }
    }

    // Hydrate initial history
    sync.hydrate()
    renderHistory(sync.state.value)

    println()
    println("Commands: send <msg> | wait [ms] | history | status | stress <n> | reset | quit")
    println()

    while (true) {
        print("> ")
        val line = readlnOrNull()?.trim() ?: break
        if (line.isEmpty()) continue

        val (cmd, arg) = line.split(" ", limit = 2).let {
            it[0] to (it.getOrNull(1) ?: "")
        }

        try {
            when (cmd) {
                "send" -> {
                    if (arg.isBlank()) { println("usage: send <message>"); continue }
                    val otid = sync.send(arg)
                    println("  sent (otid=${otid.take(20)}...)")
                }
                "wait" -> {
                    val ms = arg.toLongOrNull() ?: 10_000L
                    println("  waiting ${ms}ms...")
                    delay(ms)
                    println("  done")
                }
                "history" -> renderHistory(sync.state.value)
                "status" -> renderStatus(sync.state.value)
                "stress" -> {
                    val n = arg.toIntOrNull() ?: 5
                    println("  stress testing with $n CONCURRENT sends...")
                    // Truly parallel — simulates user rapidly tapping send
                    val jobs = (1..n).map { i ->
                        scope.launch { sync.send("stress $i/$n") }
                    }
                    jobs.joinAll()
                    println("  ${n} sends dispatched")
                }
                "reset" -> {
                    val id = api.createConversation(agentId, name = "poc-${System.currentTimeMillis()}")
                    println("  new conversation: $id (restart CLI with: $id)")
                }
                "quit", "exit" -> {
                    println("bye")
                    break
                }
                else -> println("unknown command: $cmd")
            }
        } catch (e: Exception) {
            println("  ✗ error: ${e.message}")
        }
    }

    eventJob.cancel()
    api.close()
    scope.cancel()
    exitProcess(0)
}

private fun renderHistory(timeline: Timeline) {
    println("─── Timeline (${timeline.events.size} events) ───")
    timeline.events.forEach { event ->
        val marker = when (event) {
            is TimelineEvent.Local -> when (event.deliveryState) {
                DeliveryState.SENDING -> "⏳"
                DeliveryState.SENT -> "✓"
                DeliveryState.FAILED -> "✗"
            }
            is TimelineEvent.Confirmed -> "●"
        }
        val role = when (event) {
            is TimelineEvent.Local -> event.role.name
            is TimelineEvent.Confirmed -> event.messageType.name
        }
        val content = event.content.replace("\n", " ⏎ ").take(100)
        val posStr = "%.2f".format(event.position)
        println("  $marker [$posStr] $role: $content")
    }
    println("─────────────────────────────")
}

private fun renderStatus(timeline: Timeline) {
    val local = timeline.events.filterIsInstance<TimelineEvent.Local>()
    val confirmed = timeline.events.filterIsInstance<TimelineEvent.Confirmed>()
    println("  total:       ${timeline.events.size}")
    println("  local:       ${local.size} (pending=${local.count { it.deliveryState == DeliveryState.SENDING }}, sent=${local.count { it.deliveryState == DeliveryState.SENT }}, failed=${local.count { it.deliveryState == DeliveryState.FAILED }})")
    println("  confirmed:   ${confirmed.size}")
    println("  liveCursor:  ${timeline.liveCursor}")
}
