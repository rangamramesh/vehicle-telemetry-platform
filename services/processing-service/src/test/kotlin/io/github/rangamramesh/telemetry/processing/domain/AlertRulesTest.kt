package io.github.rangamramesh.telemetry.processing.domain

import io.github.rangamramesh.telemetry.contracts.AlertSeverity
import io.github.rangamramesh.telemetry.contracts.AlertType
import io.github.rangamramesh.telemetry.contracts.TelemetryEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AlertRulesTest {

    private fun event(
        speed: Double = 80.0,
        battery: Double = 70.0,
        temp: Double? = 30.0,
        dtc: List<String> = emptyList(),
    ) = TelemetryEvent(
        eventId = "00000000-0000-0000-0000-000000000001",
        vehicleId = "VW-ID4-001",
        recordedAtEpochMs = 0,
        receivedAtEpochMs = 0,
        speedKmh = speed,
        batteryPercent = battery,
        batteryTempC = temp,
        dtcCodes = dtc,
    )

    @Test
    fun `normal driving raises no alerts`() {
        assertThat(AlertRules.evaluate(event())).isEmpty()
    }

    @Test
    fun `low battery is a warning, very low battery is critical`() {
        assertThat(AlertRules.evaluate(event(battery = 12.0)).single().severity).isEqualTo(AlertSeverity.WARNING)
        assertThat(AlertRules.evaluate(event(battery = 3.0)).single().severity).isEqualTo(AlertSeverity.CRITICAL)
    }

    @Test
    fun `several problems raise several alerts`() {
        val types = AlertRules.evaluate(event(speed = 195.0, temp = 65.0, dtc = listOf("P0A80"))).map { it.type }
        assertThat(types).containsExactlyInAnyOrder(
            AlertType.OVERSPEED,
            AlertType.BATTERY_OVERHEAT,
            AlertType.DIAGNOSTIC_TROUBLE_CODE,
        )
    }

    @Test
    fun `missing battery temperature is not an alert`() {
        assertThat(AlertRules.evaluate(event(temp = null))).isEmpty()
    }
}
