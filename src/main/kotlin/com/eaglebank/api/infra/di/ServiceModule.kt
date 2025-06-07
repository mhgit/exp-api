package com.eaglebank.api.infra.di

import com.eaglebank.api.infra.keycloak.KeycloakService
import com.eaglebank.api.infra.persistence.UserRepository
import io.ktor.server.config.*
import org.koin.dsl.module

val serviceModule = module {
    // Keycloak service
    single {
        KeycloakService(
            serverUrl = get<ApplicationConfig>().property("keycloak.serverUrl").getString(),
            realm = get<ApplicationConfig>().property("keycloak.realm").getString(),
            clientId = get<ApplicationConfig>().property("keycloak.clientId").getString(),
            clientSecret = get<ApplicationConfig>().property("keycloak.clientSecret").getString(),
            adminUsername = get<ApplicationConfig>().property("keycloak.adminUsername").getString(),
            adminPassword = get<ApplicationConfig>().property("keycloak.adminPassword").getString()
        )
    }

    // Repositories
    single { UserRepository(get()) }
}