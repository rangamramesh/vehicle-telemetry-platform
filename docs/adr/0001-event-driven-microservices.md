# ADR 0001: Event-driven microservices with Kafka

**Status:** accepted

## Context

Vehicles send readings every second. Ingestion must stay fast even when storage or alerting is slow, and new consumers of the data (notifications, analytics) are expected later.

## Decision

Split the system into an ingestion service, a processing service and a query service, connected through Apache Kafka. Readings are keyed by vehicle id.

## Consequences

- Ingestion latency does not depend on the database.
- Per-vehicle ordering is guaranteed by the partition key.
- New consumers can be added without changing producers.
- More moving parts to run and monitor; mitigated with Docker Compose, health checks and metrics.
