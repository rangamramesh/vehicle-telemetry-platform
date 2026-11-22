plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

application {
    mainClass.set("io.github.rangamramesh.telemetry.simulator.SimulatorKt")
}
