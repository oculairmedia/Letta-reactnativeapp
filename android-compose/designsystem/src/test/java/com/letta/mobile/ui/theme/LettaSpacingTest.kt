package com.letta.mobile.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.Tag

@Tag("unit")
class LettaSpacingTest {

    @Test
    fun `screenHorizontal is 12 dp`() {
        assertEquals(12.dp, LettaSpacing.screenHorizontal)
    }

    @Test
    fun `spacing scale exposes small semantic steps`() {
        assertEquals(0.dp, LettaSpacing.none)
        assertEquals(4.dp, LettaSpacing.extraSmall)
        assertEquals(6.dp, LettaSpacing.compact)
        assertEquals(8.dp, LettaSpacing.small)
        assertEquals(12.dp, LettaSpacing.medium)
        assertEquals(16.dp, LettaSpacing.large)
    }

    @Test
    fun `cardGap is 8 dp`() {
        assertEquals(8.dp, LettaSpacing.cardGap)
    }

    @Test
    fun `sectionGap is 16 dp`() {
        assertEquals(16.dp, LettaSpacing.sectionGap)
    }

    @Test
    fun `cardGroupItemGap is 2 dp`() {
        assertEquals(2.dp, LettaSpacing.cardGroupItemGap)
    }

    @Test
    fun `innerPadding is 16 dp`() {
        assertEquals(16.dp, LettaSpacing.innerPadding)
    }

    @Test
    fun `innerPaddingSmall is 12 dp`() {
        assertEquals(12.dp, LettaSpacing.innerPaddingSmall)
    }

    @Test
    fun `dialog and sheet spacing tokens preserve reusable surface geometry`() {
        assertEquals(32.dp, LettaSpacing.actionSheetBottomPadding)
        assertEquals(40.dp, LettaSpacing.dialogOuterPadding)
    }

    @Test
    fun `iconGap is 12 dp`() {
        assertEquals(12.dp, LettaSpacing.iconGap)
    }

    @Test
    fun `chipGap is 8 dp`() {
        assertEquals(8.dp, LettaSpacing.chipGap)
    }

    @Test
    fun `list item spacing tokens match existing card rhythm`() {
        assertEquals(14.dp, LettaSpacing.listItemHorizontal)
        assertEquals(10.dp, LettaSpacing.listItemVertical)
    }
}
