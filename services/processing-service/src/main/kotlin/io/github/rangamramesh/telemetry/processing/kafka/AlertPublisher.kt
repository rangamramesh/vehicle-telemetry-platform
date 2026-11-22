package io.github.rangamramesh.telemetry.processing.kafka

import io.github.rangamramesh.telemetry.contracts.AlertEvent
import io.github.rangamramesh.telemetry.contracts.EventCodec
import io.github.rangamramesh.telemetry.contracts.Topics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class AlertPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) {
    fun publish(alert: AlertEvent) {
        kafkaTemplate.send(Topics.TELEMETRY_ALERTS, alert.vehicleId, EventCodec.encode(alert))
            .get(5, TimeUnit.SECONDS)
    }
}
