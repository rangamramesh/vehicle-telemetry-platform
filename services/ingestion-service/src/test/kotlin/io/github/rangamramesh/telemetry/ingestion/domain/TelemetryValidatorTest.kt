package io.github.rangamramesh.telemetry.ingestion.domain

import io.github.rangamramesh.telemetry.ingestion.api.TelemetryIngestRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class TelemetryValidatorTest {

    private val now = Instant.parse("2026-11-22T10:00:00Z")

    private val valid = TelemetryIngestRequest(
        vehicleId = "VW-ID4-001",
        recordedAt = now.minusSeconds(2),
        speedKmh = 88.0,
        batteryPercent = 72.5,
        batteryTempC = 30.0,
        latitude = 52.52,
        longitude = 13.40,
        dtcCodes = listOf("P0A80"),
    )

    @Test
    fun `accepts a valid reading`() {
        assertThat(TelemetryValidator.validate(valid, now)).isEmpty()
    }

    @Test
    fun `rejects missing required fields`() {
        val errors = TelemetryValidator.validate(TelemetryIngestRequest(), now)
        assertThat(errors).contains(
            "vehicleId is required",
            "speedKmh is required",
            "batteryPercent is required",
        )
        assertThat(errors).anyMatch { it.startsWith("recordedAt is required") }
    }

    @Test
    fun `rejects out-of-range battery and bad coordinates`() {
        val errors = TelemetryValidator.validate(valid.copy(batteryPercent = 140.0, latitude = 95.0), now)
        assertThat(errors).containsExactlyInAnyOrder(
            "batteryPercent must be between 0.0 and 100.0",
            "latitude must be between -90.0 and 90.0",
        )
    }

    @Test
    fun `rejects timestamps too far in the future`() {
        val errors = TelemetryValidator.validate(valid.copy(recordedAt = now.plus(Duration.ofMinutes(10))), now)
        assertThat(errors).containsExactly("recordedAt is more than 5 minutes in the future")
    }

    @Test
    fun `rejects lower-case vehicle ids and invalid DTC codes`() {
        val errors = TelemetryValidator.validate(valid.copy(vehicleId = "car 1", dtcCodes = listOf("XYZ")), now)
        assertThat(errors).hasSize(2)
    }
}
