plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3"
    application
    jacoco

}

group = "com.eaglebank.api"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
    maven {
        url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

val kotlinxSerializationVersion = "1.6.2"
val logbackVersion = "1.5.13"
val koinVersion = "4.1.0-RC1"
val ktorVersion = "3.1.3"
val kotlinVersion = "2.1.21"


val mockkVersion = "1.14.2"
dependencies {
    implementation(kotlin("stdlib"))

    // Ktor server dependencies
    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation(project.dependencies.platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.insert-koin:koin-core")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.insert-koin:koin-ktor")
    implementation("io.insert-koin:koin-logger-slf4j")
    implementation("com.typesafe:config:1.4.3")


    // Ktor security
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-sessions")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // Persistance
    val exposedVersion = "0.61.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.h2database:h2:2.2.224")


    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("io.insert-koin", "koin-ktor")
    implementation("io.insert-koin", "koin-logger-slf4j")
    testImplementation("io.insert-koin", "koin-test-junit5") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
    }

    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json")


}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)  // Tests are required before generating the report
    reports {
        xml.required.set(true)  // Enable XML report for CI tools
        csv.required.set(false)
        html.required.set(true)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**",
                    "**/infrastructure/di/**",
                    "**/Application*",
                    "**/config/**"
                )
            }
        })
    )


}



kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.eaglebank.api.application.ApplicationKt")
}