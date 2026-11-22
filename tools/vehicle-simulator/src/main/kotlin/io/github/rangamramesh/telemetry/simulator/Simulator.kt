package io.github.rangamramesh.telemetry.simulator

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

/**
 * Simulates a fleet of electric vehicles sending telemetry to the ingestion service.
 *
 * Usage: ./gradlew :tools:vehicle-simulator:run --args="--vehicles=5 --interval-ms=1000 --duration-s=60"
 */
fun main(args: Array<String>) {
    val config = SimulatorConfig.parse(args)
    println("Simulating ${config.vehicles} vehicles -> ${config.url} every ${config.intervalMs} ms")

    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    val fleet = List(config.vehicles) { index -> SimulatedVehicle("SIM-EV-%03d".format(index + 1), Random(index)) }
    val endAt = if (config.durationSeconds > 0) Instant.now().plusSeconds(config.durationSeconds) else Instant.MAX
    var sent = 0
    var failed = 0

    while (Instant.now().isBefore(endAt)) {
        val batch = fleet.map { it.nextReading() }
        val body = json.encodeToString(ListSerializer(Reading.serializer()), batch)
        val request = HttpRequest.newBuilder(URI.create("${config.url}/api/v1/telemetry/batch"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 202) sent += batch.size else {
                failed += batch.size
                println("Ingestion returned ${response.statusCode()}: ${response.body()}")
            }
        } catch (e: Exception) {
            failed += batch.size
            println("Could not reach ingestion service: ${e.message}")
        }
        print("\rSent: $sent  Failed: $failed")
        Thread.sleep(config.intervalMs)
    }
    println("\nDone. Sent $sent readings, $failed failed.")
}

private val json = Json { encodeDefaults = false }

@Serializable
data class Reading(
    val vehicleId: String,
    val recordedAt: String,
    val speedKmh: Double,
    val batteryPercent: Double,
    val batteryTempC: Double,
    val odometerKm: Double,
    val latitude: Double,
    val longitude: Double,
    val dtcCodes: List<String> = emptyList(),
)

/** A vehicle that drives around Berlin, drains its battery and occasionally reports a fault. */
class SimulatedVehicle(private val id: String, private val random: Random) {
    private var speed = random.nextDouble(20.0, 90.0)
    private var battery = random.nextDouble(20.0, 100.0)
    private var temp = random.nextDouble(20.0, 35.0)
    private var odometer = random.nextDouble(1_000.0, 80_000.0)
    private var lat = 52.52 + random.nextDouble(-0.05, 0.05)
    private var lon = 13.40 + random.nextDouble(-0.05, 0.05)

    fun nextReading(): Reading {
        speed = (speed + random.nextDouble(-8.0, 8.0)).coerceIn(0.0, 200.0)
        if (random.nextDouble() < 0.01) speed = random.nextDouble(181.0, 195.0) // rare overspeed
        battery = (battery - random.nextDouble(0.05, 0.4)).let { if (it < 2.0) 95.0 else it } // "recharge"
        temp = (temp + random.nextDouble(-0.5, 0.6)).coerceIn(15.0, 70.0)
        odometer += speed / 3600.0
        lat += random.nextDouble(-0.0005, 0.0005)
        lon += random.nextDouble(-0.0005, 0.0005)
        val dtc = if (random.nextDouble() < 0.005) listOf("P0A80") else emptyList()

        return Reading(
            vehicleId = id,
            recordedAt = Instant.now().toString(),
            speedKmh = speed.round1(),
            batteryPercent = battery.round1(),
            batteryTempC = temp.round1(),
            odometerKm = odometer.round1(),
            latitude = lat,
            longitude = lon,
            dtcCodes = dtc,
        )
    }

    private fun Double.round1() = kotlin.math.round(this * 10) / 10
}

data class SimulatorConfig(
    val url: String = "http://localhost:8081",
    val vehicles: Int = 5,
    val intervalMs: Long = 1_000,
    val durationSeconds: Long = 0,
) {
    companion object {
        fun parse(args: Array<String>): SimulatorConfig {
            val options = args.mapNotNull { arg ->
                arg.removePrefix("--").split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }.toMap()
            val defaults = SimulatorConfig()
            return SimulatorConfig(
                url = options["url"] ?: defaults.url,
                vehicles = options["vehicles"]?.toInt() ?: defaults.vehicles,
                intervalMs = options["interval-ms"]?.toLong() ?: defaults.intervalMs,
                durationSeconds = options["duration-s"]?.toLong() ?: defaults.durationSeconds,
            )
        }
    }
}
