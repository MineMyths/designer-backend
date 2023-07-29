plugins {
    kotlin("jvm") version "1.8.0"

    id("io.ktor.plugin") version "2.3.2"

    kotlin("plugin.serialization") version "1.7.22"
}

group = "me.omega"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {

    // Minestom
    implementation("dev.hollowcube:minestom-ce:dev")

    // MythStom
    implementation("me.omega:mythstom:$version")

    // Logging
    implementation("org.slf4j:slf4j-jdk14:2.0.7")

    // Ktor
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.3.2")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.2")

    // Serialization
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")

    // Add support for kotlinx courotines
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Mongo
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.10.1")
    implementation("org.mongodb:bson-kotlinx:4.10.1")

    // Reflection
    implementation("org.reflections:reflections:0.10.2")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:1.7.22")
}

kotlin {
    jvmToolchain(17)
}