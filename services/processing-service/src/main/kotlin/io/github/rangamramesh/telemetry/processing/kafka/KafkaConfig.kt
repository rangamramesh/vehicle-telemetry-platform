package io.github.rangamramesh.telemetry.processing.kafka

import io.github.rangamramesh.telemetry.contracts.Topics
import kotlinx.serialization.SerializationException
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfig {

    @Bean
    fun telemetryRawTopic(): NewTopic =
        TopicBuilder.name(Topics.TELEMETRY_RAW).partitions(Topics.PARTITIONS).replicas(1).build()

    @Bean
    fun telemetryRawDltTopic(): NewTopic =
        TopicBuilder.name(Topics.TELEMETRY_RAW_DLT).partitions(Topics.PARTITIONS).replicas(1).build()

    @Bean
    fun telemetryAlertsTopic(): NewTopic =
        TopicBuilder.name(Topics.TELEMETRY_ALERTS).partitions(Topics.PARTITIONS).replicas(1).build()

    /**
     * Retries a failed message twice (1 s apart), then moves it to the dead-letter topic so one bad
     * message never blocks the partition. Malformed JSON goes to the DLT immediately.
     * Spring Boot wires this bean into the listener container automatically.
     */
    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, String>): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            TopicPartition(Topics.TELEMETRY_RAW_DLT, record.partition())
        }
        return DefaultErrorHandler(recoverer, FixedBackOff(1_000L, 2L)).apply {
            addNotRetryableExceptions(SerializationException::class.java, IllegalArgumentException::class.java)
        }
    }
}
