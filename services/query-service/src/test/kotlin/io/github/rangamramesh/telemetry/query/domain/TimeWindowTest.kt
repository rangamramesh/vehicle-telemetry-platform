package io.github.rangamramesh.telemetry.query.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class TimeWindowTest {
    private val now = Instant.parse("2026-11-22T10:00:00Z")

    @Test
    fun `defaults to the last hour and 100 rows`() {
        val window = TimeWindow.resolve(null, null, null, now)
        assertThat(window.to).isEqualTo(now)
        assertThat(window.from).isEqualTo(now.minus(Duration.ofHours(1)))
        assertThat(window.limit).isEqualTo(100)
    }

    @Test
    fun `clamps the page size`() {
        assertThat(TimeWindow.resolve(null, null, 50_000, now).limit).isEqualTo(1_000)
        assertThat(TimeWindow.resolve(null, null, 0, now).limit).isEqualTo(1)
    }

    @Test
    fun `rejects reversed and too long ranges`() {
        assertThatThrownBy { TimeWindow.resolve(now, now.minusSeconds(1), null, now) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { TimeWindow.resolve(now.minus(Duration.ofDays(40)), now, null, now) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
