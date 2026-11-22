package io.github.rangamramesh.telemetry.ingestion.api

import java.time.Instant

/** Request body sent by a vehicle (or the simulator). Fields are nullable so we can return clear validation errors. */
data class TelemetryIngestRequest(
    val vehicleId: String? = null,
    val recordedAt: Instant? = null,
    val speedKmh: Double? = null,
    val batteryPercent: Double? = null,
    val batteryTempC: Double? = null,
    val odometerKm: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dtcCodes: List<String>? = null,
)

data class IngestResponse(val eventId: String, val vehicleId: String, val status: String = "ACCEPTED")

data class BatchIngestResponse(val accepted: Int, val eventIds: List<String>)
