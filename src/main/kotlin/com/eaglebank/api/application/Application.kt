package com.eaglebank.api.application

import com.eaglebank.api.config.DatabaseConfig
import com.eaglebank.api.infra.persistence.DatabaseFactory
import com.eaglebank.api.presentation.route.usersRoute
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.module() {

    val applicationConfig = environment.config

    try {
        val dbUrl = applicationConfig.property("database.url").getString()
        val dbDriver = applicationConfig.property("database.driver").getString()
        val dbUser = applicationConfig.property("database.user").getString()
        val dbPassword = applicationConfig.property("database.password").getString()

        val dbConfig = DatabaseConfig(
            url = dbUrl,
            driver = dbDriver,
            user = dbUser,
            password = dbPassword
        )

        DatabaseFactory.init(dbConfig)

    } catch (e: Exception) {
        e.printStackTrace() // Print the full stack trace for detailed error analysis
        throw e // Re-throw to ensure test failure and visibility of the original issue
    }



    configureSecurity(applicationConfig)
    configureSerialization() // This installs ContentNegotiation
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
        usersRoute()
        swaggerUI(path = "swagger", swaggerFile = "api-contract.yml")
    }
}

fun Application.configureOpenAPI() {
    routing {
        openAPI(path = "openapi", swaggerFile = "api-contract.yml")
    }
}

fun Application.configureSecurity(config: ApplicationConfig) {
    // This needs a conversation.  Outside development, we need vault solutions for secrets etc.
}