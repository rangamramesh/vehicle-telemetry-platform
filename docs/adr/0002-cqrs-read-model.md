# ADR 0002: Separate read side (CQRS-style)

**Status:** accepted

## Context

Writes are many small inserts; reads are dashboard queries over time ranges and latest status.

## Decision

processing-service owns the schema and writes with Spring Data JPA. query-service reads the same database with plain SQL through `JdbcClient` and a read-only connection pool. A `vehicle_status` table acts as a read model for fast "current state" queries.

## Consequences

- Each side can be scaled and optimised independently.
- Both services share one database, which couples them at the schema level. Acceptable at this size; the next step would be a separate read database fed from Kafka.
