package io.github.rangamramesh.telemetry.ingestion.api

import io.github.rangamramesh.telemetry.ingestion.domain.TelemetryMapper
import io.github.rangamramesh.telemetry.ingestion.domain.TelemetryValidator
import io.github.rangamramesh.telemetry.ingestion.kafka.TelemetryPublisher
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
@RequestMapping("/api/v1/telemetry")
class TelemetryController(
    private val publisher: TelemetryPublisher,
    private val clock: Clock,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun ingest(@RequestBody request: TelemetryIngestRequest): IngestResponse {
        val now = clock.instant()
        val errors = TelemetryValidator.validate(request, now)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val event = TelemetryMapper.toEvent(request, receivedAt = now)
        publisher.publish(event)
        return IngestResponse(eventId = event.eventId, vehicleId = event.vehicleId)
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun ingestBatch(@RequestBody requests: List<TelemetryIngestRequest>): BatchIngestResponse {
        if (requests.isEmpty()) throw ValidationException(listOf("batch must contain at least one reading"))
        if (requests.size > MAX_BATCH_SIZE) throw ValidationException(listOf("batch is limited to $MAX_BATCH_SIZE readings"))

        val now = clock.instant()
        val errors = requests.flatMapIndexed { index, request ->
            TelemetryValidator.validate(request, now).map { "[$index] $it" }
        }
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val events = requests.map { TelemetryMapper.toEvent(it, receivedAt = now) }
        events.forEach(publisher::publish)
        return BatchIngestResponse(accepted = events.size, eventIds = events.map { it.eventId })
    }

    private companion object {
        const val MAX_BATCH_SIZE = 500
    }
}
