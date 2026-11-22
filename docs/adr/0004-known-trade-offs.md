# ADR 0004: Known trade-offs

**Status:** accepted, to revisit

## Alert publishing

Alerts are saved in the database transaction and published to Kafka after the commit. If Kafka is unavailable at that exact moment, the alert exists in the database but is not published, and a retry of the message will see the event as already processed.

**Planned fix:** transactional outbox. Write alerts to an `outbox` table in the same transaction and publish them from a separate poller (or Debezium CDC), marking them as sent.

## Shared database

query-service reads tables owned by processing-service. A schema change must be coordinated. Acceptable for now; see ADR 0002.

## No authentication

APIs are open for local development. Production would add Spring Security as an OAuth2 resource server validating JWTs, with separate scopes for vehicles (write) and dashboards (read).
