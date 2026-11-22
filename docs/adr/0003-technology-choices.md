# ADR 0003: Technology choices

**Status:** accepted

| Choice | Reason |
|---|---|
| Kotlin 2.3 + Spring Boot 4.1 | Concise, null-safe code; Spring Boot 4 has first-class Kotlin support and a Kotlin 2.2+ baseline |
| Java 25 (LTS) with virtual threads | Simple blocking code that scales to many concurrent requests without reactive complexity |
| Kafka 4.1 in KRaft mode | Industry standard for event streaming; no ZooKeeper to operate |
| PostgreSQL 18 + Flyway | Reliable relational store with versioned, reviewable schema migrations |
| kotlinx.serialization for events | Compile-time generated, fast, no reflection; independent of the web framework's JSON library |
| Gradle multi-module + version catalog | One build, consistent versions, shared contract library |
