package com.letta.mobile.ui.theme

import androidx.compose.ui.unit.dp
import com.letta.mobile.ui.icons.LettaIconSizing
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.Tag

@Tag("unit")
class LettaGeometryTokenTest {

    @Test
    fun `corner radius tokens preserve current component geometry`() {
        assertEquals(4.dp, LettaCornerRadius.extraSmall)
        assertEquals(8.dp, LettaCornerRadius.small)
        assertEquals(10.dp, LettaCornerRadius.listCompactAvatar)
        assertEquals(12.dp, LettaCornerRadius.medium)
        assertEquals(16.dp, LettaCornerRadius.large)
        assertEquals(20.dp, LettaCornerRadius.extraLarge)
        assertEquals(50.dp, LettaCornerRadius.pill)
    }

    @Test
    fun `component size tokens preserve chat and list geometry`() {
        assertEquals(44.dp, LettaSizing.prominentAvatar)
        assertEquals(30.dp, LettaSizing.compactAvatar)
        assertEquals(36.dp, LettaSizing.chatComposerAttachButton)
        assertEquals(64.dp, LettaSizing.chatAttachmentThumbnail)
        assertEquals(20.dp, LettaSizing.chatAttachmentRemoveButton)
    }

    @Test
    fun `elevation tokens preserve current surface emphasis levels`() {
        assertEquals(0.dp, LettaElevation.none)
        assertEquals(1.dp, LettaElevation.subtle)
        assertEquals(2.dp, LettaElevation.low)
        assertEquals(3.dp, LettaElevation.medium)
    }

    @Test
    fun `icon sizing tokens cover avatar and small overlay icons`() {
        assertEquals(22.dp, LettaIconSizing.ListAvatar)
        assertEquals(18.dp, LettaIconSizing.CompactAvatar)
        assertEquals(12.dp, LettaIconSizing.Small)
    }
}
