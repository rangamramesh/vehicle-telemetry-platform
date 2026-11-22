rootProject.name = "vehicle-telemetry-platform"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Downloads the right JDK automatically if Java 25 is not installed
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "libs:telemetry-contracts",
    "services:ingestion-service",
    "services:processing-service",
    "services:query-service",
    "tools:vehicle-simulator",
)
