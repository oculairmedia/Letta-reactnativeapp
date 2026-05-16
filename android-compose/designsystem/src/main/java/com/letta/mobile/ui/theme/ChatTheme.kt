package com.letta.mobile.ui.theme

import com.letta.mobile.ui.theme.LettaCodeFont

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

data class ChatColors(
    val userBubble: Color,
    val userText: Color,
    val userRoleLabel: Color,
    val agentBubble: Color,
    val agentText: Color,
    val agentRoleLabel: Color,
    val toolBubble: Color,
    val toolEmoji: Color,
)

data class ChatTypography(
    val messageBody: TextStyle,
    val roleLabel: TextStyle,
    val codeBlock: TextStyle,
    val toolLabel: TextStyle,
    val toolDetail: TextStyle,
    val timestamp: TextStyle,
)

data class ChatShapes(
    val bubbleRadius: Dp = 12.dp,
    val codeBlockRadius: Dp = 8.dp,
    val bubble: Shape = RoundedCornerShape(12.dp),
)

data class ChatDimens(
    val bubblePaddingHorizontal: Dp = 10.dp,
    val bubblePaddingVertical: Dp = 7.dp,
    val bubbleMaxWidthFraction: Float = 0.88f,
    val messageSpacing: Dp = 2.dp,
    val groupedMessageSpacing: Dp = 2.dp,
    val ungroupedMessageSpacing: Dp = 6.dp,
    val contentPaddingHorizontal: Dp = 12.dp,
)

val LocalChatColors = staticCompositionLocalOf<ChatColors> { error("No ChatColors provided") }
// ChatTypography + ChatFontScale use `compositionLocalOf` rather than
// `staticCompositionLocalOf` so committed font-scale changes can skip
// readers whose memoized TextStyle inputs did not actually change. This
// keeps the one-shot pinch commit cheap enough, but it is not a license to
// push raw pointer-frame fontScale updates through the whole chat tree.
// Continuous text reflow during pinch has to be throttled and profiled before
// it replaces the current GPU-layer pinch preview in ChatMessageList.
val LocalChatTypography = compositionLocalOf<ChatTypography> { error("No ChatTypography provided") }
val LocalChatShapes = staticCompositionLocalOf { ChatShapes() }
val LocalChatDimens = staticCompositionLocalOf { ChatDimens() }
val LocalChatFontScale = compositionLocalOf { 1f }
// letta-mobile-5e0f.r2: signals an active pinch-to-zoom gesture.
// animateContentSize sites in the chat tree gate themselves on this so
// we don't get cascading 150ms height interpolations across many
// bubbles per pinch frame (the actual source of the residual flicker
// after fontScale memoization).
val LocalChatIsPinching = compositionLocalOf { false }

fun TextStyle.scaledBy(factor: Float): TextStyle {
    if (factor == 1f) return this
    return copy(
        fontSize = if (fontSize.isSpecified) (fontSize.value * factor).sp else fontSize,
        lineHeight = if (lineHeight.isSpecified) (lineHeight.value * factor).sp else lineHeight,
    )
}

@Composable
fun LettaChatTheme(
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val materialTypography = MaterialTheme.typography

    // letta-mobile-5e0f.r2: ChatColors only depends on colorScheme — keyed
    // accordingly so it doesn't churn on fontScale-only changes.
    val chatColors = remember(colorScheme) {
        ChatColors(
            userBubble = colorScheme.primaryContainer,
            userText = colorScheme.onPrimaryContainer,
            userRoleLabel = colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
            agentBubble = colorScheme.surfaceContainerLow,
            agentText = colorScheme.onSurface,
            agentRoleLabel = colorScheme.primary,
            toolBubble = colorScheme.secondaryContainer,
            toolEmoji = colorScheme.onSecondaryContainer,
        )
    }

    // letta-mobile-9hcg follow-up: unified font-scale propagation.
    //
    // Previously chatTypography pre-scaled each TextStyle by fontScale AND
    // MarkdownText separately overrode LocalDensity.fontScale internally.
    // Two channels for the same input meant the user-bubble path
    // (MarkdownText → LocalDensity override) and the active-tail path
    // (chatTypography.scaledBy → TextStyle.fontSize) recomposed through
    // different chains, so per-pointer-frame fontScale changes during a
    // pinch gesture produced sub-frame mismatches where the active tail
    // had committed at scale N+1 while the committed markdown blocks
    // were still at scale N. The user perceives this as the same
    // flicker that happens at conversation-load-in.
    //
    // Single channel: install the scaled Density at the theme root so
    // EVERY sp → px conversion inside the chat tree picks it up. The
    // chatTypography styles stay at base size; the Density does the
    // multiplication. MarkdownText drops its internal override.
    // App-level callers that previously did `.scaledBy(fontScale)`
    // manually MUST drop that — otherwise they'd double-scale (theme
    // density × manual factor).
    val chatTypography = remember(materialTypography, colorScheme) {
        ChatTypography(
            messageBody = materialTypography.bodyMedium,
            roleLabel = materialTypography.chatBubbleSender.copy(letterSpacing = 0.4.sp),
            codeBlock = TextStyle(
                fontFamily = LettaCodeFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            toolLabel = materialTypography.labelMedium,
            toolDetail = materialTypography.labelSmall.copy(
                fontFamily = LettaCodeFont,
            ),
            timestamp = materialTypography.labelSmall.copy(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
            ),
        )
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        if (fontScale == 1f) {
            baseDensity
        } else {
            Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * fontScale,
            )
        }
    }

    CompositionLocalProvider(
        LocalChatColors provides chatColors,
        LocalChatTypography provides chatTypography,
        LocalChatShapes provides ChatShapes(),
        LocalChatDimens provides ChatDimens(),
        LocalChatFontScale provides fontScale,
        LocalDensity provides scaledDensity,
        content = content,
    )
}

val MaterialTheme.chatColors: ChatColors
    @Composable @ReadOnlyComposable get() = LocalChatColors.current

val MaterialTheme.chatTypography: ChatTypography
    @Composable @ReadOnlyComposable get() = LocalChatTypography.current

val MaterialTheme.chatShapes: ChatShapes
    @Composable @ReadOnlyComposable get() = LocalChatShapes.current

val MaterialTheme.chatDimens: ChatDimens
    @Composable @ReadOnlyComposable get() = LocalChatDimens.current
