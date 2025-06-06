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
}

val kotlinx_serialization_version = "1.6.2"
val logback_version = "1.5.13"
val koin_version = "3.5.3"

dependencies {
    implementation(kotlin("stdlib"))
    
    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // Ktor security
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-sessions")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")
    
    // Testing
    testImplementation(kotlin("test"))


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
    mainClass.set("com.eaglebank.api.ApplicationKt")
}
