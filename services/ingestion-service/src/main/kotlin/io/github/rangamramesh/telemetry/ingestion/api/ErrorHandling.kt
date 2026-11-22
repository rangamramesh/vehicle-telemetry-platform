package io.github.rangamramesh.telemetry.ingestion.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class ValidationException(val errors: List<String>) : RuntimeException(errors.joinToString("; "))

/** Errors are returned as RFC 9457 Problem Details. */
@RestControllerAdvice
class ErrorHandling {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Telemetry payload is invalid").apply {
            title = "Validation failed"
            setProperty("errors", ex.errors)
        }

    @ExceptionHandler(java.util.concurrent.TimeoutException::class, java.util.concurrent.ExecutionException::class)
    fun handleKafkaUnavailable(ex: Exception): ProblemDetail {
        log.error("Could not publish telemetry to Kafka", ex)
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Event stream is temporarily unavailable, retry later")
            .apply { title = "Service unavailable" }
    }
}
