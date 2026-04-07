package com.letta.mobile.ui.components

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

class DateSeparatorTest : WordSpec({
    "formatRelativeDate" should {
        "return Today for the current date" {
            formatRelativeDate(LocalDate.now()) shouldBe "Today"
        }

        "return Yesterday for the previous date" {
            formatRelativeDate(LocalDate.now().minusDays(1)) shouldBe "Yesterday"
        }

        "show month and day for same-year dates" {
            val today = LocalDate.now()
            val date = today.minusDays(10)
            if (date.year == today.year) {
                val result = formatRelativeDate(date)
                result shouldNotContain date.year.toString()
                result shouldContain date.dayOfMonth.toString()
            }
        }

        "show full date for different-year dates" {
            val result = formatRelativeDate(LocalDate.of(2023, 3, 15))
            result shouldContain "2023"
            result shouldContain "15"
        }
    }
})
