package com.letta.mobile.platform.root

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Flavor-gated bridge for executing commands through an external Android `su`
 * provider. Implementations must fail closed outside the root flavor.
 */
interface RootShellBridge {
    /** Cheap, non-interactive availability snapshot suitable for capability UI. */
    fun peekAvailability(): RootShellAvailability

    /** Performs a bounded, non-interactive probe for an installed `su` provider. */
    suspend fun detect(): RootShellAvailability

    /** Triggers the external root manager prompt by running a harmless root command. */
    suspend fun requestGrant(timeoutMs: Long = RootShellDefaults.GRANT_TIMEOUT_MS): RootShellGrantResult

    /** Executes a command through `su`, respecting coroutine cancellation and request limits. */
    suspend fun execute(request: RootShellCommandRequest): RootShellCommandResult
}

data class RootShellAvailability(
    val status: RootShellAvailabilityStatus,
    val providerHint: String? = null,
    val reason: String,
) {
    val mayRequestGrant: Boolean
        get() = status == RootShellAvailabilityStatus.SuAvailable ||
            status == RootShellAvailabilityStatus.NeedsDetection
}

enum class RootShellAvailabilityStatus {
    UnsupportedBuild,
    NeedsDetection,
    SuUnavailable,
    SuAvailable,
}

data class RootShellCommandRequest(
    val command: String,
    val cwd: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val timeoutMs: Long = RootShellDefaults.COMMAND_TIMEOUT_MS,
    val outputLimitBytes: Int = RootShellDefaults.OUTPUT_LIMIT_BYTES,
) {
    init {
        require(command.isNotBlank()) { "Root command must not be blank." }
        require(timeoutMs > 0) { "Root command timeout must be positive." }
        require(outputLimitBytes > 0) { "Root command output limit must be positive." }
        require(environment.keys.all(::isSafeEnvironmentKey)) {
            "Root command environment keys must match [A-Za-z_][A-Za-z0-9_]*."
        }
    }
}

data class RootShellGrantResult(
    val granted: Boolean,
    val uid: String?,
    val result: RootShellCommandResult,
) {
    val providerHint: String? = result.providerHint
}

data class RootShellCommandResult(
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val stdoutTruncated: Boolean,
    val stderrTruncated: Boolean,
    val timedOut: Boolean,
    val durationMs: Long,
    val providerHint: String? = null,
    val errorMessage: String? = null,
) {
    val succeeded: Boolean
        get() = exitCode == 0 && !timedOut && errorMessage == null
}

object RootShellDefaults {
    const val COMMAND_TIMEOUT_MS: Long = 30_000
    const val GRANT_TIMEOUT_MS: Long = 20_000
    const val OUTPUT_LIMIT_BYTES: Int = 64 * 1024
}

@Singleton
class NoopRootShellBridge @Inject constructor() : RootShellBridge {
    override fun peekAvailability(): RootShellAvailability = unsupported()

    override suspend fun detect(): RootShellAvailability = unsupported()

    override suspend fun requestGrant(timeoutMs: Long): RootShellGrantResult = RootShellGrantResult(
        granted = false,
        uid = null,
        result = unavailableResult("Root shell bridge is not compiled into this flavor."),
    )

    override suspend fun execute(request: RootShellCommandRequest): RootShellCommandResult =
        unavailableResult("Root command execution is disabled for this flavor.")

    private fun unsupported() = RootShellAvailability(
        status = RootShellAvailabilityStatus.UnsupportedBuild,
        reason = "Root shell bridge is only available in the root distribution flavor.",
    )

    private fun unavailableResult(reason: String) = RootShellCommandResult(
        exitCode = null,
        stdout = "",
        stderr = "",
        stdoutTruncated = false,
        stderrTruncated = false,
        timedOut = false,
        durationMs = 0,
        errorMessage = reason,
    )
}

internal fun isSafeEnvironmentKey(key: String): Boolean =
    key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))

internal fun Throwable.rootShellMessage(): String = when (this) {
    is CancellationException -> "Root command was cancelled."
    else -> message ?: javaClass.simpleName
}
