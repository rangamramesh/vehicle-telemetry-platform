package io.github.rangamramesh.telemetry.contracts

/** Kafka topic names shared by all services. The version suffix allows breaking schema changes later. */
object Topics {
    const val TELEMETRY_RAW = "telemetry.raw.v1"
    const val TELEMETRY_RAW_DLT = "telemetry.raw.v1.dlt"
    const val TELEMETRY_ALERTS = "telemetry.alerts.v1"

    /** Same partition count everywhere so the dead-letter topic can mirror the source partition. */
    const val PARTITIONS = 3
}
