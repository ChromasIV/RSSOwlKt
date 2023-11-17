plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "com.chromasgaming"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.prof18.rssparser:rssparser:6.0.4")

    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("org.slf4j:slf4j-api:2.0.9")

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.mysql:mysql-connector-j:8.2.0")


    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.chromasgaming.MainKt")
}