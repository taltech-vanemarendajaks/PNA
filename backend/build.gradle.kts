plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
    kotlin("plugin.serialization") version "2.0.20"
    application
}

group = "com.pna"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.pna.backend.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-openapi-jvm")
    implementation("io.ktor:ktor-server-swagger-jvm")

    implementation("com.google.api-client:google-api-client:2.7.0")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.55")
    implementation("com.googlecode.libphonenumber:carrier:1.239")
    implementation("com.googlecode.libphonenumber:geocoder:2.249")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")

    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
