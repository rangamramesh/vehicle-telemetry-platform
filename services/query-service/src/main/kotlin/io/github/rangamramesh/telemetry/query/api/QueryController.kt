package io.github.rangamramesh.telemetry.query.api

import io.github.rangamramesh.telemetry.query.domain.TimeWindow
import io.github.rangamramesh.telemetry.query.persistence.TelemetryQueryRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant

@RestController
@RequestMapping("/api/v1")
class QueryController(
    private val repository: TelemetryQueryRepository,
    private val clock: Clock,
) {

    @GetMapping("/vehicles")
    fun vehicles(): List<VehicleSummary> = repository.listVehicles(clock.instant())

    @GetMapping("/vehicles/{vehicleId}/status")
    fun status(@PathVariable vehicleId: String): VehicleStatusView =
        repository.findStatus(vehicleId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No telemetry received yet for vehicle $vehicleId")

    @GetMapping("/vehicles/{vehicleId}/readings")
    fun readings(
        @PathVariable vehicleId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @RequestParam(required = false) limit: Int?,
    ): ReadingsPage {
        val window = TimeWindow.resolve(from, to, limit, clock.instant())
        val readings = repository.findReadings(vehicleId, window)
        return ReadingsPage(vehicleId, window.from, window.to, readings.size, readings)
    }

    @GetMapping("/alerts")
    fun alerts(
        @RequestParam(required = false) vehicleId: String?,
        @RequestParam(required = false) severity: String?,
        @RequestParam(required = false) limit: Int?,
    ): List<AlertView> {
        val size = (limit ?: TimeWindow.DEFAULT_LIMIT).coerceIn(1, TimeWindow.MAX_LIMIT)
        return repository.findAlerts(vehicleId, severity?.uppercase(), size)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request")
}
