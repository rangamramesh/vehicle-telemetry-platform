# API reference

All endpoints use JSON. Times are ISO-8601 in UTC, e.g. `2026-11-22T10:15:30Z`. Errors follow RFC 9457 *Problem Details*.

## Ingestion service (port 8081)

### `POST /api/v1/telemetry` – send one reading

```json
{
  "vehicleId": "VW-ID4-001",
  "recordedAt": "2026-11-22T10:15:30Z",
  "speedKmh": 87.5,
  "batteryPercent": 64.0,
  "batteryTempC": 31.2,
  "odometerKm": 15230.4,
  "latitude": 52.52,
  "longitude": 13.405,
  "dtcCodes": ["P0A80"]
}
```

| Field | Required | Rules |
|---|---|---|
| `vehicleId` | yes | 3–32 characters, `A-Z`, `0-9`, `-` |
| `recordedAt` | yes | not more than 5 min in the future, not older than 7 days |
| `speedKmh` | yes | 0–400 |
| `batteryPercent` | yes | 0–100 |
| `batteryTempC` | no | -40–120 |
| `odometerKm` | no | 0–5,000,000 |
| `latitude` / `longitude` | no | valid coordinates |
| `dtcCodes` | no | OBD-II format, e.g. `P0A80` |

**202 Accepted**

```json
{ "eventId": "3f0c8a7e-...", "vehicleId": "VW-ID4-001", "status": "ACCEPTED" }
```

**400 Bad Request**

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Telemetry payload is invalid",
  "errors": ["vehicleId must be 3-32 characters: A-Z, 0-9 or '-'", "speedKmh must be between 0.0 and 400.0"]
}
```

**503 Service Unavailable** – Kafka is not reachable; retry later.

### `POST /api/v1/telemetry/batch` – send up to 500 readings

Body: a JSON array of the objects above. All readings are validated first; if any is invalid, nothing is published and the errors are prefixed with the index, e.g. `[3] batteryPercent is required`.

**202 Accepted**

```json
{ "accepted": 2, "eventIds": ["...", "..."] }
```

## Query service (port 8083)

### `GET /api/v1/vehicles`

```json
[
  { "vehicleId": "SIM-EV-001", "lastSeenAt": "2026-11-22T10:15:30Z", "batteryPercent": 64.2, "openAlertsLast24h": 3 }
]
```

### `GET /api/v1/vehicles/{vehicleId}/status`

```json
{
  "vehicleId": "SIM-EV-001",
  "lastSeenAt": "2026-11-22T10:15:30Z",
  "speedKmh": 72.4,
  "batteryPercent": 64.2,
  "batteryTempC": 29.8,
  "odometerKm": 41233.7,
  "latitude": 52.521,
  "longitude": 13.402
}
```

`404` if the vehicle has never sent data.

### `GET /api/v1/vehicles/{vehicleId}/readings?from=&to=&limit=`

| Parameter | Default | Notes |
|---|---|---|
| `from` | `to` minus 1 hour | ISO-8601 |
| `to` | now | ISO-8601 |
| `limit` | 100 | 1–1000 |

Range is limited to 31 days. Readings are returned newest first.

```json
{
  "vehicleId": "SIM-EV-001",
  "from": "2026-11-22T09:15:30Z",
  "to": "2026-11-22T10:15:30Z",
  "count": 1,
  "readings": [
    { "eventId": "...", "recordedAt": "2026-11-22T10:15:30Z", "speedKmh": 72.4, "batteryPercent": 64.2,
      "batteryTempC": 29.8, "latitude": 52.521, "longitude": 13.402, "dtcCodes": [] }
  ]
}
```

### `GET /api/v1/alerts?vehicleId=&severity=&limit=`

All parameters are optional. `severity` is `INFO`, `WARNING` or `CRITICAL`.

```json
[
  { "alertId": "...", "vehicleId": "SIM-EV-003", "type": "LOW_BATTERY", "severity": "WARNING",
    "message": "Battery low: 13.4%", "recordedAt": "2026-11-22T10:15:30Z" }
]
```

## Operations endpoints (all services)

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Overall health |
| `/actuator/health/liveness`, `/actuator/health/readiness` | Kubernetes-style probes |
| `/actuator/prometheus` | Metrics for Prometheus |
| `/actuator/metrics` | Metrics browser |
