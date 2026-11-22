package io.github.rangamramesh.telemetry.query.api

import java.time.Instant

data class VehicleSummary(
    val vehicleId: String,
    val lastSeenAt: Instant,
    val batteryPercent: Double,
    val openAlertsLast24h: Int,
)

data class VehicleStatusView(
    val vehicleId: String,
    val lastSeenAt: Instant,
    val speedKmh: Double,
    val batteryPercent: Double,
    val batteryTempC: Double?,
    val odometerKm: Double?,
    val latitude: Double?,
    val longitude: Double?,
)

data class ReadingView(
    val eventId: String,
    val recordedAt: Instant,
    val speedKmh: Double,
    val batteryPercent: Double,
    val batteryTempC: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val dtcCodes: List<String>,
)

data class AlertView(
    val alertId: String,
    val vehicleId: String,
    val type: String,
    val severity: String,
    val message: String,
    val recordedAt: Instant,
)

data class ReadingsPage(val vehicleId: String, val from: Instant, val to: Instant, val count: Int, val readings: List<ReadingView>)
