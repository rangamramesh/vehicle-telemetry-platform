# Getting started

This guide takes you from a fresh laptop to a running platform, step by step. It works on Windows, macOS and Linux.

## 1. Install the prerequisites

| Tool | Why | How to check |
|---|---|---|
| **Git** | Download the code | `git --version` |
| **Docker Desktop** (or Docker Engine + Compose v2) | Runs Kafka, PostgreSQL, Kafka UI, Prometheus | `docker compose version` |
| **JDK 25** *(optional)* | Builds and runs the services. If missing, Gradle downloads it automatically | `java -version` |
| **IntelliJ IDEA** *(optional)* | Best Kotlin IDE; Community Edition is free | |
| **curl** | Calls the APIs from the terminal (preinstalled on macOS, Linux and Windows 10+) | `curl --version` |

Downloads:
- Git: https://git-scm.com/downloads
- Docker Desktop: https://www.docker.com/products/docker-desktop/
- JDK 25 (Temurin): https://adoptium.net/

You do **not** need to install Gradle, Kafka or PostgreSQL yourself. The Gradle wrapper (`gradlew`) and Docker handle that.

Make sure Docker Desktop is **running** before you continue (the whale icon in the menu bar or system tray).

## 2. Get the code

```bash
git clone https://github.com/rangamramesh/vehicle-telemetry-platform.git
cd vehicle-telemetry-platform
```

On macOS/Linux, make the wrapper executable once:

```bash
chmod +x gradlew
```

> **Windows:** in every command below, replace `./gradlew` with `gradlew.bat`.

## 3. Start the infrastructure

```bash
docker compose up -d
```

This starts four containers:

| Container | Port | Purpose |
|---|---|---|
| `vt-kafka` | 9092 | Apache Kafka broker (KRaft mode) |
| `vt-postgres` | 5432 | PostgreSQL, database `telemetry`, user/password `telemetry` |
| `vt-kafka-ui` | 8090 | Web UI to browse topics and messages |
| `vt-prometheus` | 9090 | Collects metrics from the services |

Wait until Kafka and PostgreSQL report **healthy** (about 20–40 seconds):

```bash
docker compose ps
```

## 4. Build and run the tests

```bash
./gradlew build
```

The first build downloads Gradle, Java 25 (if needed) and all libraries, so it can take a few minutes. You should end with `BUILD SUCCESSFUL`.

## 5. Start the services

Open **three terminals** in the project folder and run one command in each:

```bash
# Terminal 1 – receives telemetry and publishes it to Kafka
./gradlew :services:ingestion-service:bootRun

# Terminal 2 – consumes from Kafka, stores in PostgreSQL, raises alerts
./gradlew :services:processing-service:bootRun

# Terminal 3 – REST API to read the stored data
./gradlew :services:query-service:bootRun
```

Each service is ready when you see `Started ...Application in ... seconds`.

Start **processing-service before query-service** the first time: it creates the database tables (Flyway migration).

Check that all three are up:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

Each should return `{"status":"UP"}`.

> **In IntelliJ:** open the project folder, wait for the Gradle import, then run `IngestionApplication`, `ProcessingApplication` and `QueryApplication` with the green ▶ icon next to `main`.

## 6. Send your first reading

```bash
curl -i -X POST http://localhost:8081/api/v1/telemetry \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleId": "VW-ID4-001",
    "recordedAt": "2026-11-22T10:15:30Z",
    "speedKmh": 87.5,
    "batteryPercent": 12.0,
    "batteryTempC": 31.2,
    "latitude": 52.52,
    "longitude": 13.405,
    "dtcCodes": ["P0A80"]
  }'
```

> Replace the `recordedAt` value with the current UTC time; readings more than 5 minutes in the future or older than 7 days are rejected.

You get `HTTP/1.1 202 Accepted` and an `eventId`. Battery at 12% and a trouble code each raise an alert.

## 7. Run the fleet simulator

In a fourth terminal:

```bash
./gradlew :tools:vehicle-simulator:run --args="--vehicles=10 --interval-ms=1000"
```

Options:

| Option | Default | Meaning |
|---|---|---|
| `--vehicles` | 5 | Number of simulated cars (`SIM-EV-001`, `SIM-EV-002`, …) |
| `--interval-ms` | 1000 | Time between readings |
| `--duration-s` | 0 | Stop after N seconds (0 = run until Ctrl+C) |
| `--url` | http://localhost:8081 | Ingestion service address |

## 8. Look at the results

```bash
# All vehicles with their battery level and alert count
curl http://localhost:8083/api/v1/vehicles

# Latest status of one vehicle
curl http://localhost:8083/api/v1/vehicles/SIM-EV-001/status

# Last 20 readings
curl "http://localhost:8083/api/v1/vehicles/SIM-EV-001/readings?limit=20"

# Latest alerts, or only critical ones
curl http://localhost:8083/api/v1/alerts
curl "http://localhost:8083/api/v1/alerts?severity=CRITICAL"
```

More examples: [api.md](api.md) and [`http/requests.http`](../http/requests.http).

**Kafka UI:** open http://localhost:8090 → *Topics* → `telemetry.raw.v1` → *Messages* to see the raw events, and *Consumers* → `processing-service` to see consumer lag.

**Prometheus:** open http://localhost:9090 → *Status* → *Targets*. Try the query `http_server_requests_seconds_count`.

**Database:** connect any SQL client to `localhost:5432`, database/user/password `telemetry`, or use:

```bash
docker exec -it vt-postgres psql -U telemetry -d telemetry -c "SELECT vehicle_id, battery_percent, last_seen_at FROM vehicle_status;"
```

## 9. Alternative: run everything in Docker

```bash
./gradlew bootJar
docker compose --profile apps up -d --build
```

The services then run as containers on the same ports (8081–8083). Logs:

```bash
docker compose logs -f processing-service
```

## 10. Stop and clean up

```bash
# Stop the services: Ctrl+C in each terminal (or, for Docker:)
docker compose --profile apps down

# Stop the infrastructure but keep the data
docker compose down

# Stop and delete all data
docker compose down -v
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `Cannot connect to the Docker daemon` | Start Docker Desktop and wait until it says *running*. |
| `port is already allocated` (5432, 9092, 8081…) | Another program uses the port. Stop it (e.g. a local PostgreSQL), or change the left side of the port mapping in `compose.yaml`. |
| Services log `Connection to node -1 could not be established` | Kafka is not ready yet. Check `docker compose ps` and wait for *healthy*. |
| `relation "vehicle_status" does not exist` in query-service | Start processing-service once; it creates the tables. |
| Ingestion returns `503 Service unavailable` | Kafka is down or unreachable. |
| Ingestion returns `400` | Read the `errors` list in the response; usually `recordedAt` is in the wrong format or too far in the future. |
| `./gradlew: Permission denied` | Run `chmod +x gradlew`. |
| Gradle cannot find Java 25 | Install Temurin 25, or check that your network allows the automatic download. |
| Build is slow the first time | Normal: dependencies are downloaded once and cached. |

## Run only the tests

```bash
./gradlew test                                   # all modules
./gradlew :services:processing-service:test      # one module
```

Test reports: `<module>/build/reports/tests/test/index.html`.
