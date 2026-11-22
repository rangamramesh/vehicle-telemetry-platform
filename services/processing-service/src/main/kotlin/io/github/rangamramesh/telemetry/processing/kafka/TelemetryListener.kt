package io.github.rangamramesh.telemetry.processing.kafka

import io.github.rangamramesh.telemetry.contracts.EventCodec
import io.github.rangamramesh.telemetry.contracts.Topics
import io.github.rangamramesh.telemetry.processing.domain.TelemetryProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class TelemetryListener(
    private val processor: TelemetryProcessor,
    private val alertPublisher: AlertPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [Topics.TELEMETRY_RAW], groupId = "processing-service", concurrency = "3")
    fun onTelemetry(record: ConsumerRecord<String, String>) {
        val event = EventCodec.decodeTelemetry(record.value())
        val alerts = processor.process(event) // transaction commits here
        alerts.forEach(alertPublisher::publish)
        if (alerts.isNotEmpty()) {
            log.info("Vehicle {} raised {} alert(s)", event.vehicleId, alerts.size)
        }
    }
}
