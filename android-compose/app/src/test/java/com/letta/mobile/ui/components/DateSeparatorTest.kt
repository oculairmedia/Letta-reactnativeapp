package com.letta.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateSeparatorTest {

    @Test
    fun `today returns Today`() {
        assertEquals("Today", formatRelativeDate(LocalDate.now()))
    }

    @Test
    fun `yesterday returns Yesterday`() {
        assertEquals("Yesterday", formatRelativeDate(LocalDate.now().minusDays(1)))
    }

    @Test
    fun `same year shows month and day`() {
        val today = LocalDate.now()
        val date = today.minusDays(10)
        if (date.year == today.year) {
            val result = formatRelativeDate(date)
            assert(!result.contains(date.year.toString())) { "Same-year date should not contain year: $result" }
            assert(result.contains(date.dayOfMonth.toString())) { "Should contain day: $result" }
        }
    }

    @Test
    fun `different year shows full date`() {
        val date = LocalDate.of(2023, 3, 15)
        val result = formatRelativeDate(date)
        assert(result.contains("2023")) { "Different-year date should contain year: $result" }
        assert(result.contains("15")) { "Should contain day: $result" }
    }
}
