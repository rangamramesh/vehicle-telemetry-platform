package io.github.rangamramesh.telemetry.ingestion.kafka

import io.github.rangamramesh.telemetry.contracts.Topics
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/** Spring's KafkaAdmin creates these topics on startup if they do not exist yet. */
@Configuration
class KafkaTopicsConfig {
    @Bean
    fun telemetryRawTopic(): NewTopic =
        TopicBuilder.name(Topics.TELEMETRY_RAW).partitions(Topics.PARTITIONS).replicas(1).build()
}
