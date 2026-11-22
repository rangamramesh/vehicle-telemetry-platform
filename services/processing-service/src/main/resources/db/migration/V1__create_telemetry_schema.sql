-- Owned by processing-service (write side). query-service only reads these tables.

CREATE TABLE telemetry_readings (
    event_id        UUID PRIMARY KEY,
    vehicle_id      VARCHAR(32)      NOT NULL,
    recorded_at     TIMESTAMPTZ      NOT NULL,
    received_at     TIMESTAMPTZ      NOT NULL,
    speed_kmh       DOUBLE PRECISION NOT NULL,
    battery_percent DOUBLE PRECISION NOT NULL,
    battery_temp_c  DOUBLE PRECISION,
    odometer_km     DOUBLE PRECISION,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    dtc_codes       TEXT
);

-- Most queries ask for one vehicle in a time range, newest first
CREATE INDEX idx_readings_vehicle_time ON telemetry_readings (vehicle_id, recorded_at DESC);

CREATE TABLE vehicle_status (
    vehicle_id      VARCHAR(32) PRIMARY KEY,
    last_seen_at    TIMESTAMPTZ      NOT NULL,
    speed_kmh       DOUBLE PRECISION NOT NULL,
    battery_percent DOUBLE PRECISION NOT NULL,
    battery_temp_c  DOUBLE PRECISION,
    odometer_km     DOUBLE PRECISION,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    updated_at      TIMESTAMPTZ      NOT NULL
);

CREATE TABLE alerts (
    alert_id        UUID PRIMARY KEY,
    vehicle_id      VARCHAR(32)  NOT NULL,
    type            VARCHAR(40)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL,
    message         VARCHAR(500) NOT NULL,
    recorded_at     TIMESTAMPTZ  NOT NULL,
    source_event_id UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_alerts_vehicle_time ON alerts (vehicle_id, recorded_at DESC);
CREATE INDEX idx_alerts_time ON alerts (recorded_at DESC);
