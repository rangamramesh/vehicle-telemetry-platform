package io.github.rangamramesh.telemetry.processing.domain

import io.github.rangamramesh.telemetry.contracts.AlertEvent
import io.github.rangamramesh.telemetry.contracts.TelemetryEvent
import io.github.rangamramesh.telemetry.processing.persistence.AlertEntity
import io.github.rangamramesh.telemetry.processing.persistence.AlertRepository
import io.github.rangamramesh.telemetry.processing.persistence.TelemetryReadingEntity
import io.github.rangamramesh.telemetry.processing.persistence.TelemetryReadingRepository
import io.github.rangamramesh.telemetry.processing.persistence.VehicleStatusEntity
import io.github.rangamramesh.telemetry.processing.persistence.VehicleStatusRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Stores a reading, updates the vehicle's latest status and creates alerts, all in one transaction.
 * Returns the alerts so the caller can publish them after the transaction has committed.
 */
@Service
class TelemetryProcessor(
    private val readings: TelemetryReadingRepository,
    private val statuses: VehicleStatusRepository,
    private val alerts: AlertRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun process(event: TelemetryEvent): List<AlertEvent> {
        val eventId = UUID.fromString(event.eventId)
        if (readings.existsById(eventId)) {
            log.info("Skipping duplicate event {}", eventId) // Kafka delivers at least once
            return emptyList()
        }

        val now = clock.instant()
        val recordedAt = Instant.ofEpochMilli(event.recordedAtEpochMs)
        readings.save(event.toReadingEntity(eventId, recordedAt))
        updateStatus(event, recordedAt, now)

        return AlertRules.evaluate(event).map { candidate ->
            val alert = AlertEntity(
                alertId = UUID.randomUUID(),
                vehicleId = event.vehicleId,
                type = candidate.type.name,
                severity = candidate.severity.name,
                message = candidate.message,
                recordedAt = recordedAt,
                sourceEventId = eventId,
                createdAt = now,
            )
            alerts.save(alert)
            AlertEvent(
                alertId = alert.alertId.toString(),
                vehicleId = alert.vehicleId,
                type = candidate.type,
                severity = candidate.severity,
                message = alert.message,
                recordedAtEpochMs = event.recordedAtEpochMs,
                sourceEventId = event.eventId,
            )
        }
    }

    /** Only newer readings change the status, so late or out-of-order events never overwrite fresh data. */
    private fun updateStatus(event: TelemetryEvent, recordedAt: Instant, now: Instant) {
        val existing = statuses.findById(event.vehicleId).orElse(null)
        if (existing == null) {
            statuses.save(
                VehicleStatusEntity(
                    vehicleId = event.vehicleId,
                    lastSeenAt = recordedAt,
                    speedKmh = event.speedKmh,
                    batteryPercent = event.batteryPercent,
                    batteryTempC = event.batteryTempC,
                    odometerKm = event.odometerKm,
                    latitude = event.latitude,
                    longitude = event.longitude,
                    updatedAt = now,
                ),
            )
        } else if (recordedAt.isAfter(existing.lastSeenAt)) {
            existing.apply {
                lastSeenAt = recordedAt
                speedKmh = event.speedKmh
                batteryPercent = event.batteryPercent
                batteryTempC = event.batteryTempC
                odometerKm = event.odometerKm
                latitude = event.latitude
                longitude = event.longitude
                updatedAt = now
            }
        }
    }

    private fun TelemetryEvent.toReadingEntity(id: UUID, recordedAt: Instant) = TelemetryReadingEntity(
        eventId = id,
        vehicleId = vehicleId,
        recordedAt = recordedAt,
        receivedAt = Instant.ofEpochMilli(receivedAtEpochMs),
        speedKmh = speedKmh,
        batteryPercent = batteryPercent,
        batteryTempC = batteryTempC,
        odometerKm = odometerKm,
        latitude = latitude,
        longitude = longitude,
        dtcCodes = dtcCodes.takeIf { it.isNotEmpty() }?.joinToString(","),
    )
}
