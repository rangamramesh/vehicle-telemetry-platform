package io.github.rangamramesh.telemetry.processing.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Every accepted reading. The event id is the primary key, which makes processing idempotent. */
@Entity
@Table(name = "telemetry_readings")
class TelemetryReadingEntity(
    @Id
    @Column(name = "event_id")
    val eventId: UUID,
    @Column(name = "vehicle_id", nullable = false)
    val vehicleId: String,
    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant,
    @Column(name = "received_at", nullable = false)
    val receivedAt: Instant,
    @Column(name = "speed_kmh", nullable = false)
    val speedKmh: Double,
    @Column(name = "battery_percent", nullable = false)
    val batteryPercent: Double,
    @Column(name = "battery_temp_c")
    val batteryTempC: Double?,
    @Column(name = "odometer_km")
    val odometerKm: Double?,
    @Column(name = "latitude")
    val latitude: Double?,
    @Column(name = "longitude")
    val longitude: Double?,
    @Column(name = "dtc_codes")
    val dtcCodes: String?,
)

/** Latest known state per vehicle (a read model for fast "current status" queries). */
@Entity
@Table(name = "vehicle_status")
class VehicleStatusEntity(
    @Id
    @Column(name = "vehicle_id")
    val vehicleId: String,
    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant,
    @Column(name = "speed_kmh", nullable = false)
    var speedKmh: Double,
    @Column(name = "battery_percent", nullable = false)
    var batteryPercent: Double,
    @Column(name = "battery_temp_c")
    var batteryTempC: Double?,
    @Column(name = "odometer_km")
    var odometerKm: Double?,
    @Column(name = "latitude")
    var latitude: Double?,
    @Column(name = "longitude")
    var longitude: Double?,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)

@Entity
@Table(name = "alerts")
class AlertEntity(
    @Id
    @Column(name = "alert_id")
    val alertId: UUID,
    @Column(name = "vehicle_id", nullable = false)
    val vehicleId: String,
    @Column(name = "type", nullable = false)
    val type: String,
    @Column(name = "severity", nullable = false)
    val severity: String,
    @Column(name = "message", nullable = false)
    val message: String,
    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant,
    @Column(name = "source_event_id", nullable = false)
    val sourceEventId: UUID,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
