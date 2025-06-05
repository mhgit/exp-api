package com.eaglebank.api.application

import com.eaglebank.api.infrastructure.di.serviceModule
import com.eaglebank.api.presentation.routes.usersRoute
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import com.typesafe.config.Config
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import com.eaglebank.api.infrastructure.security.configureSecurity


fun Application.module(config: Config) {
    install(Koin) {
        slf4jLogger()
        modules(serviceModule)
    }

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
        usersRoute()
        swaggerUI(path = "swagger", swaggerFile = "api-contract.yml")
    }
}

fun Application.configureOpenAPI() {
    routing {
        openAPI(path = "openapi", swaggerFile = "api-contract.yml")
    }
}