package io.github.rangamramesh.telemetry.contracts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EventCodecTest {

    private val event = TelemetryEvent(
        eventId = "7d6c3f2e-1111-4a4a-9b9b-000000000001",
        vehicleId = "VW-ID4-001",
        recordedAtEpochMs = 1_760_000_000_000,
        receivedAtEpochMs = 1_760_000_000_100,
        speedKmh = 87.5,
        batteryPercent = 64.0,
        batteryTempC = 31.2,
        dtcCodes = listOf("P0A80"),
    )

    @Test
    fun `telemetry event survives an encode-decode round trip`() {
        val decoded = EventCodec.decodeTelemetry(EventCodec.encode(event))
        assertThat(decoded).isEqualTo(event)
    }

    @Test
    fun `unknown fields from newer producers are ignored`() {
        val payload = EventCodec.encode(event).replace("}", ",\"newField\":42}")
        assertThat(EventCodec.decodeTelemetry(payload)).isEqualTo(event)
    }

    @Test
    fun `alert event survives an encode-decode round trip`() {
        val alert = AlertEvent(
            alertId = "a1",
            vehicleId = "VW-ID4-001",
            type = AlertType.LOW_BATTERY,
            severity = AlertSeverity.CRITICAL,
            message = "Battery at 4%",
            recordedAtEpochMs = 1_760_000_000_000,
            sourceEventId = event.eventId,
        )
        assertThat(EventCodec.decodeAlert(EventCodec.encode(alert))).isEqualTo(alert)
    }
}
