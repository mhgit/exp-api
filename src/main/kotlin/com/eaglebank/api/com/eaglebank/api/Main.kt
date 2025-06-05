package com.eaglebank.api

import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import com.typesafe.config.Config
import java.io.File
import com.eaglebank.api.infrastructure.security.configureSecurity


fun main() {
    val config = ConfigFactory.systemProperties()
        .withFallback(ConfigFactory.systemEnvironment())
        .withFallback(ConfigFactory.load())
        .withFallback(ConfigFactory.parseFile(File("/etc/eagle-bank/application.conf")))
        .resolve()

    embeddedServer(
        Netty,
        port = config.getInt("ktor.deployment.port"),
        host = "0.0.0.0",
        module = { module(config) }
    ).start(wait = true)

}


fun Application.module(config: Config) {
    configureSerialization()
    configureSecurity(config)
    configureRouting()
    configureOpenAPI()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureRouting() {
    routing {
        // Routes will be added here
        swaggerUI(path = "swagger", swaggerFile = "api-contract.yml")
    }
}

fun Application.configureOpenAPI() {
    routing {
        openAPI(path = "openapi", swaggerFile = "api-contract.yml")
    }
}