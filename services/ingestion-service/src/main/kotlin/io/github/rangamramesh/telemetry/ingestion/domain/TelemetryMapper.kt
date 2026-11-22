package io.github.rangamramesh.telemetry.ingestion.domain

import io.github.rangamramesh.telemetry.contracts.TelemetryEvent
import io.github.rangamramesh.telemetry.ingestion.api.TelemetryIngestRequest
import java.time.Instant
import java.util.UUID

object TelemetryMapper {
    /** Call only after [TelemetryValidator] returned no errors. */
    fun toEvent(request: TelemetryIngestRequest, receivedAt: Instant, eventId: UUID = UUID.randomUUID()) =
        TelemetryEvent(
            eventId = eventId.toString(),
            vehicleId = requireNotNull(request.vehicleId),
            recordedAtEpochMs = requireNotNull(request.recordedAt).toEpochMilli(),
            receivedAtEpochMs = receivedAt.toEpochMilli(),
            speedKmh = requireNotNull(request.speedKmh),
            batteryPercent = requireNotNull(request.batteryPercent),
            batteryTempC = request.batteryTempC,
            odometerKm = request.odometerKm,
            latitude = request.latitude,
            longitude = request.longitude,
            dtcCodes = request.dtcCodes.orEmpty().distinct(),
        )
}
