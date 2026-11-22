package io.github.rangamramesh.telemetry.processing.domain

import io.github.rangamramesh.telemetry.contracts.AlertSeverity
import io.github.rangamramesh.telemetry.contracts.AlertType
import io.github.rangamramesh.telemetry.contracts.TelemetryEvent

data class AlertCandidate(val type: AlertType, val severity: AlertSeverity, val message: String)

/** Stateless alert rules. New rules can be added here without touching Kafka or the database code. */
object AlertRules {
    const val LOW_BATTERY_WARNING = 15.0
    const val LOW_BATTERY_CRITICAL = 5.0
    const val BATTERY_OVERHEAT_C = 60.0
    const val OVERSPEED_KMH = 180.0

    fun evaluate(event: TelemetryEvent): List<AlertCandidate> = buildList {
        when {
            event.batteryPercent < LOW_BATTERY_CRITICAL -> add(
                AlertCandidate(AlertType.LOW_BATTERY, AlertSeverity.CRITICAL, "Battery critically low: ${fmt(event.batteryPercent)}%"),
            )
            event.batteryPercent < LOW_BATTERY_WARNING -> add(
                AlertCandidate(AlertType.LOW_BATTERY, AlertSeverity.WARNING, "Battery low: ${fmt(event.batteryPercent)}%"),
            )
        }

        event.batteryTempC?.takeIf { it > BATTERY_OVERHEAT_C }?.let {
            add(AlertCandidate(AlertType.BATTERY_OVERHEAT, AlertSeverity.CRITICAL, "Battery temperature ${fmt(it)} °C"))
        }

        if (event.speedKmh > OVERSPEED_KMH) {
            add(AlertCandidate(AlertType.OVERSPEED, AlertSeverity.WARNING, "Speed ${fmt(event.speedKmh)} km/h"))
        }

        if (event.dtcCodes.isNotEmpty()) {
            add(
                AlertCandidate(
                    AlertType.DIAGNOSTIC_TROUBLE_CODE,
                    AlertSeverity.WARNING,
                    "Diagnostic trouble codes: ${event.dtcCodes.joinToString()}",
                ),
            )
        }
    }

    private fun fmt(value: Double) = "%.1f".format(java.util.Locale.ROOT, value)
}
