package com.letta.mobile.feature.chat

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.letta.mobile.data.model.MessageContentPart
import com.letta.mobile.ui.theme.LettaTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Tag
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@Tag("unit")
class ChatComposerAttachmentThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `composer renders attached image bitmap instead of empty placeholder`() {
        composeRule.setContent {
            LettaTheme {
                ChatComposer(
                    inputText = "",
                    pendingAttachments = persistentListOf(testImage()),
                    isStreaming = false,
                    canSendMessages = true,
                    onTextChange = {},
                    onSend = {},
                    onStop = {},
                    onRemoveAttachment = {},
                    onAttachImage = {},
                )
            }
        }

        composeRule.onNodeWithTag(ChatComposerTestTags.AttachmentThumbnailImage)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ChatComposerTestTags.AttachmentThumbnailPlaceholder)
            .assertDoesNotExist()
    }

    private fun testImage(): MessageContentPart.Image {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.MAGENTA)
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return MessageContentPart.Image(
            base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP),
            mediaType = "image/png",
        )
    }
}
