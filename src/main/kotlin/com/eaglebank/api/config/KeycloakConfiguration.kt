package com.eaglebank.api.config

import io.ktor.server.config.*

data class KeycloakConfig(
    val serverUrl: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String,
    val adminUsername: String,
    val adminPassword: String
) {
    companion object {
        fun fromConfig(config: ApplicationConfig): KeycloakConfig {
            val keycloakConfig = config.config("keycloak")
            return KeycloakConfig(
                serverUrl = keycloakConfig.property("serverUrl").getString(),
                realm = keycloakConfig.property("realm").getString(),
                clientId = keycloakConfig.property("clientId").getString(),
                clientSecret = keycloakConfig.property("clientSecret").getString(),
                adminUsername = keycloakConfig.property("adminUsername").getString(),
                adminPassword = keycloakConfig.property("adminPassword").getString()
            )
        }
    }
}