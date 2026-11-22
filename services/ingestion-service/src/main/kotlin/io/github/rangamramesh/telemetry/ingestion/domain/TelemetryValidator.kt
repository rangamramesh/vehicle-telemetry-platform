package io.github.rangamramesh.telemetry.ingestion.domain

import io.github.rangamramesh.telemetry.ingestion.api.TelemetryIngestRequest
import java.time.Duration
import java.time.Instant

/** Pure validation rules, kept free of Spring so they are easy to unit test. */
object TelemetryValidator {
    private val VEHICLE_ID = Regex("^[A-Z0-9-]{3,32}$")
    private val DTC_CODE = Regex("^[PCBU][0-9A-F]{4}$")
    private val MAX_CLOCK_SKEW: Duration = Duration.ofMinutes(5)
    private val MAX_AGE: Duration = Duration.ofDays(7)

    fun validate(request: TelemetryIngestRequest, now: Instant): List<String> = buildList {
        val vehicleId = request.vehicleId
        when {
            vehicleId.isNullOrBlank() -> add("vehicleId is required")
            !VEHICLE_ID.matches(vehicleId) -> add("vehicleId must be 3-32 characters: A-Z, 0-9 or '-'")
        }

        val recordedAt = request.recordedAt
        when {
            recordedAt == null -> add("recordedAt is required (ISO-8601, e.g. 2026-11-22T10:15:30Z)")
            recordedAt.isAfter(now.plus(MAX_CLOCK_SKEW)) -> add("recordedAt is more than 5 minutes in the future")
            recordedAt.isBefore(now.minus(MAX_AGE)) -> add("recordedAt is older than 7 days")
        }

        checkRange("speedKmh", request.speedKmh, 0.0, 400.0, required = true)
        checkRange("batteryPercent", request.batteryPercent, 0.0, 100.0, required = true)
        checkRange("batteryTempC", request.batteryTempC, -40.0, 120.0)
        checkRange("odometerKm", request.odometerKm, 0.0, 5_000_000.0)
        checkRange("latitude", request.latitude, -90.0, 90.0)
        checkRange("longitude", request.longitude, -180.0, 180.0)

        request.dtcCodes.orEmpty()
            .filterNot { DTC_CODE.matches(it) }
            .forEach { add("dtcCodes contains invalid code '$it' (expected e.g. P0A80)") }
    }

    private fun MutableList<String>.checkRange(
        field: String,
        value: Double?,
        min: Double,
        max: Double,
        required: Boolean = false,
    ) {
        when {
            value == null -> if (required) add("$field is required")
            value.isNaN() || value < min || value > max -> add("$field must be between $min and $max")
        }
    }
}
