package io.github.rangamramesh.telemetry.contracts

import kotlinx.serialization.json.Json

/** JSON encoding for Kafka messages. Unknown fields are ignored so producers can add fields safely. */
object EventCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(event: TelemetryEvent): String = json.encodeToString(TelemetryEvent.serializer(), event)

    fun encode(event: AlertEvent): String = json.encodeToString(AlertEvent.serializer(), event)

    fun decodeTelemetry(payload: String): TelemetryEvent = json.decodeFromString(TelemetryEvent.serializer(), payload)

    fun decodeAlert(payload: String): AlertEvent = json.decodeFromString(AlertEvent.serializer(), payload)
}
