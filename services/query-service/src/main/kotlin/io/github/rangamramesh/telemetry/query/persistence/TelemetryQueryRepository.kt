package io.github.rangamramesh.telemetry.query.persistence

import io.github.rangamramesh.telemetry.query.api.AlertView
import io.github.rangamramesh.telemetry.query.api.ReadingView
import io.github.rangamramesh.telemetry.query.api.VehicleStatusView
import io.github.rangamramesh.telemetry.query.api.VehicleSummary
import io.github.rangamramesh.telemetry.query.domain.TimeWindow
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Read-only SQL against the tables written by processing-service (CQRS read side). */
@Repository
class TelemetryQueryRepository(private val jdbc: JdbcClient) {

    fun listVehicles(now: Instant): List<VehicleSummary> =
        jdbc.sql(
            """
            SELECT s.vehicle_id, s.last_seen_at, s.battery_percent,
                   (SELECT COUNT(*) FROM alerts a
                     WHERE a.vehicle_id = s.vehicle_id AND a.recorded_at >= :since) AS alerts_24h
            FROM vehicle_status s
            ORDER BY s.vehicle_id
            """.trimIndent(),
        )
            .param("since", now.minusSeconds(24 * 3600).toOffset())
            .query { rs, _ ->
                VehicleSummary(
                    vehicleId = rs.getString("vehicle_id"),
                    lastSeenAt = rs.instant("last_seen_at"),
                    batteryPercent = rs.getDouble("battery_percent"),
                    openAlertsLast24h = rs.getInt("alerts_24h"),
                )
            }
            .list()

    fun findStatus(vehicleId: String): VehicleStatusView? =
        jdbc.sql("SELECT * FROM vehicle_status WHERE vehicle_id = :vehicleId")
            .param("vehicleId", vehicleId)
            .query { rs, _ ->
                VehicleStatusView(
                    vehicleId = rs.getString("vehicle_id"),
                    lastSeenAt = rs.instant("last_seen_at"),
                    speedKmh = rs.getDouble("speed_kmh"),
                    batteryPercent = rs.getDouble("battery_percent"),
                    batteryTempC = rs.doubleOrNull("battery_temp_c"),
                    odometerKm = rs.doubleOrNull("odometer_km"),
                    latitude = rs.doubleOrNull("latitude"),
                    longitude = rs.doubleOrNull("longitude"),
                )
            }
            .optional()
            .orElse(null)

    fun findReadings(vehicleId: String, window: TimeWindow): List<ReadingView> =
        jdbc.sql(
            """
            SELECT event_id, recorded_at, speed_kmh, battery_percent, battery_temp_c,
                   latitude, longitude, dtc_codes
            FROM telemetry_readings
            WHERE vehicle_id = :vehicleId AND recorded_at BETWEEN :from AND :to
            ORDER BY recorded_at DESC
            LIMIT :limit
            """.trimIndent(),
        )
            .param("vehicleId", vehicleId)
            .param("from", window.from.toOffset())
            .param("to", window.to.toOffset())
            .param("limit", window.limit)
            .query { rs, _ ->
                ReadingView(
                    eventId = rs.getString("event_id"),
                    recordedAt = rs.instant("recorded_at"),
                    speedKmh = rs.getDouble("speed_kmh"),
                    batteryPercent = rs.getDouble("battery_percent"),
                    batteryTempC = rs.doubleOrNull("battery_temp_c"),
                    latitude = rs.doubleOrNull("latitude"),
                    longitude = rs.doubleOrNull("longitude"),
                    dtcCodes = rs.getString("dtc_codes")?.split(",").orEmpty(),
                )
            }
            .list()

    fun findAlerts(vehicleId: String?, severity: String?, limit: Int): List<AlertView> =
        jdbc.sql(
            """
            SELECT alert_id, vehicle_id, type, severity, message, recorded_at
            FROM alerts
            WHERE (CAST(:vehicleId AS TEXT) IS NULL OR vehicle_id = :vehicleId)
              AND (CAST(:severity AS TEXT) IS NULL OR severity = :severity)
            ORDER BY recorded_at DESC
            LIMIT :limit
            """.trimIndent(),
        )
            .param("vehicleId", vehicleId)
            .param("severity", severity)
            .param("limit", limit)
            .query { rs, _ ->
                AlertView(
                    alertId = rs.getString("alert_id"),
                    vehicleId = rs.getString("vehicle_id"),
                    type = rs.getString("type"),
                    severity = rs.getString("severity"),
                    message = rs.getString("message"),
                    recordedAt = rs.instant("recorded_at"),
                )
            }
            .list()

    private fun Instant.toOffset(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

    private fun ResultSet.instant(column: String): Instant =
        getObject(column, OffsetDateTime::class.java).toInstant()

    private fun ResultSet.doubleOrNull(column: String): Double? {
        val value = getDouble(column)
        return if (wasNull()) null else value
    }
}
