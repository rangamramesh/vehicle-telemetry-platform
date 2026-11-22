package io.github.rangamramesh.telemetry.contracts

import kotlinx.serialization.Serializable

/**
 * One vehicle telemetry reading, as published to [Topics.TELEMETRY_RAW].
 * Kafka key = [vehicleId], so all readings of one vehicle stay in order on one partition.
 */
@Serializable
data class TelemetryEvent(
    val eventId: String,
    val vehicleId: String,
    val recordedAtEpochMs: Long,
    val receivedAtEpochMs: Long,
    val speedKmh: Double,
    val batteryPercent: Double,
    val batteryTempC: Double? = null,
    val odometerKm: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dtcCodes: List<String> = emptyList(),
    val schemaVersion: Int = 1,
)

@Serializable
enum class AlertSeverity { INFO, WARNING, CRITICAL }

@Serializable
enum class AlertType { LOW_BATTERY, BATTERY_OVERHEAT, OVERSPEED, DIAGNOSTIC_TROUBLE_CODE }

/** An alert raised by the processing service, published to [Topics.TELEMETRY_ALERTS]. */
@Serializable
data class AlertEvent(
    val alertId: String,
    val vehicleId: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val recordedAtEpochMs: Long,
    val sourceEventId: String,
)
