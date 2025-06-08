package com.eaglebank.api.infra.di

import com.eaglebank.api.config.DatabaseConfig
import com.eaglebank.api.config.KeycloakConfig
import com.eaglebank.api.domain.repository.IUserRepository
import com.eaglebank.api.infra.keycloak.KeycloakService
import com.eaglebank.api.infra.persistence.UserRepository
import com.eaglebank.api.infra.persistence.UserTable
import com.eaglebank.api.infra.validation.SimpleUserRequestValidationService
import com.eaglebank.api.infra.validation.UserRequestValidationService
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.dsl.module
import org.slf4j.LoggerFactory

fun createServiceModule(config: ApplicationConfig) = module {
    val logger = LoggerFactory.getLogger("ServiceModule")

    // Provide the ApplicationConfig
    single {
        config
    }
    
    single {
        logger.debug("Creating DatabaseConfig")
        try {
            val appConfig = get<ApplicationConfig>()
            DatabaseConfig(
                url = appConfig.property("database.url").getString(),
                driver = appConfig.property("database.driver").getString(),
                user = appConfig.property("database.user").getString(),
                password = appConfig.property("database.password").getString()
            )
        } catch (e: Exception) {
            logger.error("Failed to create DatabaseConfig", e)
            throw e
        }
    }

    single {
        logger.debug("Creating KeycloakService")
        try {
            val appConfig = get<ApplicationConfig>()
            val keycloakConfig = KeycloakConfig.fromConfig(appConfig)
            KeycloakService(
                serverUrl = keycloakConfig.serverUrl,
                realm = keycloakConfig.realm,
                clientId = keycloakConfig.clientId,
                clientSecret = keycloakConfig.clientSecret,
                adminUsername = keycloakConfig.adminUsername,
                adminPassword = keycloakConfig.adminPassword
            )
        } catch (e: Exception) {
            logger.error("Failed to create KeycloakService", e)
            throw e
        }
    }

    single {
        logger.debug("Initializing Database")
        val dbConfig = get<DatabaseConfig>()
        Database.connect(
            url = dbConfig.url,
            driver = dbConfig.driver,
            user = dbConfig.user,
            password = dbConfig.password
        ).also { db ->
            transaction(db) {
                SchemaUtils.create(UserTable)
            }
        }
    }

    single<IUserRepository> {
        logger.debug("Creating UserRepository")
        val keycloakService = get<KeycloakService>()
        get<Database>() // Ensure database is initialized
        UserRepository(keycloakService)
    }

    single<UserRequestValidationService> {
        SimpleUserRequestValidationService()
    }
}