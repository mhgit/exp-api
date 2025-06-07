package com.eaglebank.api.infra.di

import com.eaglebank.api.config.KeycloakConfig
import com.eaglebank.api.infra.keycloak.KeycloakService
import io.ktor.server.application.*
import org.koin.dsl.module

val keycloakModule = module {
    single { KeycloakConfig.fromConfig(get<Application>().environment.config) }
    single {
        val config = get<KeycloakConfig>()
        KeycloakService(
            serverUrl = config.serverUrl,
            realm = config.realm,
            clientId = config.clientId,
            clientSecret = config.clientSecret,
            adminUsername = config.adminUsername,
            adminPassword = config.adminPassword
        )
    }
}