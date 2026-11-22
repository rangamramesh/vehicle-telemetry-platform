package io.github.rangamramesh.telemetry.ingestion.kafka

import io.github.rangamramesh.telemetry.contracts.EventCodec
import io.github.rangamramesh.telemetry.contracts.TelemetryEvent
import io.github.rangamramesh.telemetry.contracts.Topics
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class TelemetryPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Publishes synchronously (with a timeout) so that an HTTP 202 means the event is stored in Kafka.
     * Blocking is cheap here because the service runs on virtual threads.
     */
    fun publish(event: TelemetryEvent) {
        kafkaTemplate.send(Topics.TELEMETRY_RAW, event.vehicleId, EventCodec.encode(event))
            .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        log.debug("Published event {} for vehicle {}", event.eventId, event.vehicleId)
    }

    private companion object {
        const val SEND_TIMEOUT_SECONDS = 5L
    }
}
