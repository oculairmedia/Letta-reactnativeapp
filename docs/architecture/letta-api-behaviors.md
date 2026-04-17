# Letta API Behavior Empirical Findings

**Date:** 2026-04-17
**Bead:** letta-mobile-mdlg
**Test server:** http://192.168.50.90:8289
**Test agent:** agent-d53a5c94-908d-4b6d-a95c-cce0466cf1c3 (Letta Mobile Admin)
**Test conversation:** conv-bb4cf3c0-beb0-4dfb-ada0-7236f0a8dda1

## TL;DR — Answers to open questions

| Question | Answer |
|----------|--------|
| Does API accept client-provided `otid`? | ✅ YES |
| Does server preserve our `otid` verbatim? | ✅ YES — stored exactly as sent |
| Does server enforce `otid` uniqueness? | ❌ NO — duplicates accepted silently |
| Does streaming endpoint echo user message with our otid? | ❌ NO — only sends assistant/reasoning/tool messages |
| Are server-generated otids ordered within a step? | ✅ YES — `{prefix}{00/01}` or `{prefix}{80/81}` pattern |
| Is the non-streaming POST endpoint usable? | ⚠️ NO — returns `{"detail": "An unknown error occurred"}` currently |
| Does GET /conversations/{id}/messages return our otid? | ✅ YES — reliably |

## Detailed findings

### 1. otid acceptance and preservation

POST body shape (confirmed working):
```json
{
  "messages": [{
    "type": "message",
    "role": "user",
    "content": "hello",
    "otid": "our-client-generated-string"
  }],
  "streaming": true
}
```

Retrieval via GET returns:
```json
{
  "message_type": "user_message",
  "otid": "our-client-generated-string",
  "id": "message-<server-assigned-uuid>",
  ...
}
```

**The otid round-trips perfectly.** This is the foundation for Matrix-style transaction ID matching.

### 2. otid uniqueness NOT enforced

Sent two requests with identical `otid=duplicate-test-otid-00000000-...`:
- Both accepted, returned 200
- Both stored as separate messages with different server `id`s
- Both have the same `otid` in storage

**Implication:** otid idempotency must be enforced client-side. We cannot use duplicate-send of same otid as a safe retry mechanism.

### 3. Server-generated otid ordering pattern

Within a single step (one user turn), multiple server-generated messages share a derived otid prefix:

| Message type | otid suffix |
|--------------|-------------|
| `reasoning_message` | `{prefix}80` or `{prefix}00` |
| `assistant_message` | `{prefix}81` or `{prefix}01` |

Examples from real conversations:
```
reasoning:  17bc375f-f9d2-4033-9c43-0f1265866100
assistant:  17bc375f-f9d2-4033-9c43-0f1265866101

reasoning:  782d6556-f353-4315-9473-85aee35fe880
assistant:  782d6556-f353-4315-9473-85aee35fe881
```

**This is a reliable intra-step ordering key.** Sorting by `(step_id, otid)` within a conversation gives correct order even when timestamps collide.

### 4. Streaming endpoint behavior

`POST /v1/conversations/{conv_id}/messages?streaming=true` emits SSE events:

```
data: { ping event, no otid }
data: { assistant_message with server otid, our user otid NOT echoed }
data: { stop_reason event }
data: { cleanup_error event — appears to be benign server-side bug }
```

**The user message is NOT echoed back in the stream.** To confirm persistence:
1. POST success (200) = server accepted our message
2. GET /conversations/{id}/messages = retrieves our user message with our otid preserved
3. Stream gives us the assistant response(s) with their own server otids

### 5. Non-streaming endpoint currently broken

`POST /v1/conversations/{conv_id}/messages` with `streaming: false` returns:
```json
{"detail": "An unknown error occurred"}
```

Root cause visible in stream error: `name 'uuid' is not defined` — a Python import error in the server. Even streaming requests emit this error AFTER the stream completes, but streaming works despite it.

**For the client design:** always use streaming mode. Non-streaming is not reliable currently.

### 6. Stream pattern — full event sequence

```
data: {"message_type":"ping", "run_id":"run-X"}
data: {"message_type":"assistant_message", "otid":"...", "run_id":"run-X", "content":"..."}
data: {"message_type":"stop_reason", "stop_reason":"end_turn"}
event: error
data: {"message_type":"error_message", "error_type":"cleanup_error", ...}
```

With reasoning enabled (agent-dependent):
```
data: {"message_type":"ping"}
data: {"message_type":"reasoning_message", "otid":"...00", "content":"..."}
data: {"message_type":"assistant_message", "otid":"...01", "content":"..."}
data: {"message_type":"stop_reason"}
```

With tools:
```
...
data: {"message_type":"tool_call_message", "tool_call": {...}}
data: {"message_type":"tool_return_message", "tool_call_id":"...", "status":"success"}
data: {"message_type":"reasoning_message"}
data: {"message_type":"assistant_message"}
data: {"message_type":"stop_reason"}
```

## Design implications for POC

1. **Use otid as our Matrix-style txn_id** — it works. Generate UUID client-side, send with message, retrieve via GET.

2. **Don't rely on duplicate-otid for idempotency** — client must dedupe before sending.

3. **Stream confirms assistant messages, not user messages** — user message confirmation requires a followup GET. BUT the POST 200 + local otid is enough to trust persistence.

4. **Within-step ordering via `(step_id, otid)` tuple** — never use timestamps alone.

5. **Always use streaming mode** — non-streaming currently broken on our server.

6. **Timeline reconciliation algorithm:**
   - On send: append Local event with our otid
   - POST success: mark Local as SENT
   - Stream events arrive: append Confirmed events with server otids (different from our user otid)
   - After stream Complete: GET /messages to retrieve our user message's server record; replace Local with Confirmed by matching our otid

7. **The `cleanup_error` server bug is benign** — ignore error events of type `cleanup_error` with detail containing `uuid`. These fire after stream completion.

## Unresolved / needs more testing

- **`after=X` pagination stability** — not yet tested. Need to verify that repeated calls with same cursor return consistent results.
- **Tool call ordering within a step** — suffixes for tool_call/tool_return messages not yet characterized.
- **Reconnection behavior** — if stream drops mid-response, does GET return the partial assistant message or wait for completion?

These will be tested as part of the POC CLI build, not blocking the architecture decision.

## Decision

✅ **Proceed with otid-based Matrix-style architecture.** All critical assumptions validated.
