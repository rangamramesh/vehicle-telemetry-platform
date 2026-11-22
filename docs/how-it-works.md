# How it works: a code walkthrough

Follow one telemetry reading through the system, file by file. Use this to understand the code and to explain it in interviews.

## 1. The shared contract

`libs/telemetry-contracts`

- **`Events.kt`** defines `TelemetryEvent` and `AlertEvent`. These are the only data that travels between services, so all services compile against the same definition.
- **`Topics.kt`** holds the topic names and partition count in one place.
- **`EventCodec.kt`** turns events into JSON with kotlinx.serialization. `ignoreUnknownKeys = true` means an older consumer does not break when a newer producer adds a field (forward compatibility).

Why a library and not copy-paste? One source of truth for the message format. The trade-off is that services share a build dependency, which is acceptable inside one repository.

## 2. Receiving the reading

`services/ingestion-service`

1. **`TelemetryController.ingest`** receives the HTTP request. Jackson 3 turns the JSON body into a `TelemetryIngestRequest`.
2. **`TelemetryValidator.validate`** checks every field and returns a list of all errors, not just the first, which is friendlier for API clients. It is a plain Kotlin object with the current time passed in, so the tests need no Spring and no fake clocks.
3. **`TelemetryMapper.toEvent`** creates the `TelemetryEvent` with a new UUID as `eventId` and the server's receive time.
4. **`TelemetryPublisher.publish`** sends it to Kafka with the vehicle id as the key and **waits for the acknowledgement**. Only then does the controller answer `202 Accepted`.
5. **`ErrorHandling`** turns validation errors into `400` and Kafka problems into `503`, both as Problem Details JSON.

Waiting for Kafka blocks a thread. With `spring.threads.virtual.enabled: true` every request runs on a cheap virtual thread, so blocking is fine and the code stays simple compared with reactive code.

## 3. Processing the reading

`services/processing-service`

1. **`TelemetryListener.onTelemetry`** is called by Spring Kafka for each message (`@KafkaListener`, 3 concurrent consumers, one per partition).
2. **`TelemetryProcessor.process`** runs in one database transaction:
   - skips the event if its id is already stored (Kafka guarantees *at-least-once* delivery, so duplicates are possible),
   - saves the reading (`TelemetryReadingEntity`),
   - updates `VehicleStatusEntity` only if this reading is newer than the stored status,
   - evaluates **`AlertRules`** and saves an `AlertEntity` for each hit.
3. After the transaction commits, the listener publishes each alert to `telemetry.alerts.v1` via **`AlertPublisher`**.
4. **`KafkaConfig`** creates the topics and the error handler: two retries, then the dead-letter topic. Malformed JSON (`SerializationException`) goes straight to the dead-letter topic.
5. **Flyway** runs `db/migration/V1__create_telemetry_schema.sql` on startup. Hibernate does not create tables (`ddl-auto: none`); the schema is versioned like code.

## 4. Reading the data

`services/query-service`

- **`QueryController`** exposes the GET endpoints.
- **`TimeWindow.resolve`** turns optional `from`, `to` and `limit` into a safe, bounded query, so a client cannot ask for millions of rows.
- **`TelemetryQueryRepository`** uses Spring's `JdbcClient` with plain SQL. The read side does not need JPA entities; SQL is clearer and faster for queries and keeps this service independent of the write model.
- The connection pool is set to read-only (`hikari.read-only: true`).

## 5. Running and observing

- **`compose.yaml`** starts Kafka (KRaft mode, two listeners so it works from your laptop and from containers), PostgreSQL, Kafka UI and Prometheus. The `apps` profile also starts the services.
- **Actuator** exposes health and Prometheus metrics on every service; **`infra/prometheus/prometheus.yml`** scrapes them.
- **`.github/workflows/ci.yml`** builds, tests and builds the Docker images on every push.

## Interview talking points

- **Why Kafka?** Decouples receiving from processing, absorbs traffic spikes, keeps per-vehicle ordering via partition keys, and lets new consumers (e.g. notifications, analytics) read the same stream later.
- **Exactly-once?** Kafka gives at-least-once delivery here; the consumer is idempotent through the event id primary key, which gives an effectively-once result in the database.
- **Why CQRS-style separation?** Write and read workloads scale differently; the query service can be scaled or cached without touching processing.
- **What would you improve?** Transactional outbox for alerts, authentication, Testcontainers integration tests, TimescaleDB for long-term storage, Kubernetes deployment.
