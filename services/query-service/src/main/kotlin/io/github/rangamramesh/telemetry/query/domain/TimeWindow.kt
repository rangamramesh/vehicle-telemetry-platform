package io.github.rangamramesh.telemetry.query.domain

import java.time.Duration
import java.time.Instant

/** Resolves optional query parameters into a safe, bounded time window and page size. */
data class TimeWindow(val from: Instant, val to: Instant, val limit: Int) {
    companion object {
        val DEFAULT_RANGE: Duration = Duration.ofHours(1)
        val MAX_RANGE: Duration = Duration.ofDays(31)
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 1_000

        fun resolve(from: Instant?, to: Instant?, limit: Int?, now: Instant): TimeWindow {
            val end = to ?: now
            val start = from ?: end.minus(DEFAULT_RANGE)
            require(!start.isAfter(end)) { "'from' must be before 'to'" }
            require(Duration.between(start, end) <= MAX_RANGE) { "time range is limited to 31 days" }
            val size = (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
            return TimeWindow(start, end, size)
        }
    }
}
