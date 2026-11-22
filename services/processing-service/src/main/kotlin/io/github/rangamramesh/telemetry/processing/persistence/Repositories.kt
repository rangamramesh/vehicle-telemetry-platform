package io.github.rangamramesh.telemetry.processing.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TelemetryReadingRepository : JpaRepository<TelemetryReadingEntity, UUID>

interface VehicleStatusRepository : JpaRepository<VehicleStatusEntity, String>

interface AlertRepository : JpaRepository<AlertEntity, UUID>
