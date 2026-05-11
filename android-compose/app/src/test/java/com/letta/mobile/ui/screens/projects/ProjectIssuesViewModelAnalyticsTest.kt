package com.letta.mobile.ui.screens.projects

import com.letta.mobile.data.model.ProjectIssueSummary
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectIssuesViewModelAnalyticsTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun `completed timeline includes closed style statuses sorted by update time`() {
        val issues = listOf(
            issue(
                id = "open",
                status = "open",
                updatedAt = "2026-04-03T10:00:00Z",
            ),
            issue(
                id = "closed-old",
                status = "closed",
                updatedAt = "2026-04-01T10:00:00Z",
            ),
            issue(
                id = "done-new",
                status = "in_progress",
                statusLabel = "Done",
                updatedAt = "2026-04-04T10:00:00Z",
            ),
        )

        val timeline = buildCompletedIssueTimeline(issues, zoneId = utc)

        assertEquals(listOf("done-new", "closed-old"), timeline.map { it.id })
    }

    @Test
    fun `issue creation buckets group by local date and keep the newest buckets`() {
        val issues = listOf(
            issue(id = "one", createdAt = "2026-04-01T05:00:00Z"),
            issue(id = "two", createdAt = "2026-04-01T23:00:00Z"),
            issue(id = "three", createdAt = "2026-04-02T01:00:00Z"),
            issue(id = "four", createdAt = "2026-04-03T01:00:00Z"),
        )

        val buckets = buildIssueCreationBuckets(
            issues = issues,
            maxBuckets = 2,
            zoneId = utc,
            locale = Locale.US,
        )

        assertEquals(listOf("2026-04-02", "2026-04-03"), buckets.map { it.date })
        assertEquals(listOf(1, 1), buckets.map { it.count })
    }

    private fun issue(
        id: String,
        status: String = "open",
        statusLabel: String? = null,
        updatedAt: String? = null,
        createdAt: String? = null,
    ) = ProjectIssueSummary(
        id = id,
        projectId = "letta-mobile",
        title = "Issue $id",
        status = status,
        statusLabel = statusLabel,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )
}
