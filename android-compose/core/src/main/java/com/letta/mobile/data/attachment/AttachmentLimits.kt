package com.letta.mobile.data.attachment

/**
 * lcp-dlj: tunable caps for image attachments. Defaults follow
 * Anthropic's vision-input guidance (≤ 1568 px longest side,
 * ≤ ~2 MB per image, ≤ 4 images per turn) which keeps payloads
 * comfortably under the admin-shim's 10 MB `content_parts` hard
 * cap (MOBILE_WS_STREAMING_CONTRACT.md §"send_message content
 * parts"; shim returns `protocol_violation` if the cap is hit).
 *
 * Injected via Hilt so call sites read a single source of truth.
 * Default values bind to [Default]; swap the provider to point at
 * a [com.letta.mobile.data.repository.SettingsRepository]-backed
 * source when the settings UI lands a user-tunable surface.
 *
 * @param maxAttachmentCount  hard count cap per send.
 * @param maxLongestEdgePx    downscale target for the longest edge.
 * @param maxRawBytesPerImage post-encode byte cap per image. The
 *   encoder loops down from [initialJpegQuality] to
 *   [minJpegQuality] in [jpegQualityStep] increments until the
 *   bytes fit; if the floor doesn't fit, the encoder returns the
 *   floor encoding (caller still surfaces the resulting size to
 *   the user; the shim ultimately enforces its own cap).
 * @param maxTotalBase64Bytes cumulative cap on the composer's
 *   staged-but-not-yet-sent attachments (base64-encoded). Loose
 *   sanity check at the composer; the shim's hard cap is on
 *   `content_parts` JSON bytes.
 * @param initialJpegQuality  first JPEG quality the encoder tries.
 * @param minJpegQuality      floor for the quality-fallback loop.
 * @param jpegQualityStep     decrement per loop iteration.
 */
data class AttachmentLimits(
    val maxAttachmentCount: Int = 4,
    val maxLongestEdgePx: Int = 1568,
    val maxRawBytesPerImage: Int = 2 * 1024 * 1024,
    val maxTotalBase64Bytes: Int = 8 * 1024 * 1024,
    val initialJpegQuality: Int = 85,
    val minJpegQuality: Int = 50,
    val jpegQualityStep: Int = 10,
) {
    init {
        require(maxAttachmentCount > 0) { "maxAttachmentCount must be > 0" }
        require(maxLongestEdgePx > 0) { "maxLongestEdgePx must be > 0" }
        require(maxRawBytesPerImage > 0) { "maxRawBytesPerImage must be > 0" }
        require(maxTotalBase64Bytes > 0) { "maxTotalBase64Bytes must be > 0" }
        require(initialJpegQuality in 1..100) { "initialJpegQuality must be in 1..100" }
        require(minJpegQuality in 1..100) { "minJpegQuality must be in 1..100" }
        require(minJpegQuality <= initialJpegQuality) {
            "minJpegQuality ($minJpegQuality) must be ≤ initialJpegQuality ($initialJpegQuality)"
        }
        require(jpegQualityStep > 0) { "jpegQualityStep must be > 0" }
    }

    /**
     * Yields JPEG quality settings to try in order, from
     * [initialJpegQuality] down to (and including) [minJpegQuality],
     * decrementing by [jpegQualityStep]. The last value is always
     * exactly [minJpegQuality] so the floor is honoured even when
     * the step doesn't divide the range cleanly.
     */
    fun jpegQualityFallbackLadder(): List<Int> = buildList {
        var q = initialJpegQuality
        while (q > minJpegQuality) {
            add(q)
            q -= jpegQualityStep
        }
        add(minJpegQuality)
    }

    companion object {
        val Default = AttachmentLimits()
    }
}
