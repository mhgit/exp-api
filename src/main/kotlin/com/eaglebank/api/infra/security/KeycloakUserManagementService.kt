package com.eaglebank.api.infra.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.representations.idm.CredentialRepresentation
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

class KeycloakService(
    private val serverUrl: String,
    private val realm: String,
    private val clientId: String,
    private val clientSecret: String,
    private val adminUsername: String,
    private val adminPassword: String
) {
    private val logger = LoggerFactory.getLogger(KeycloakService::class.java)
    
    private val keycloak: Keycloak = KeycloakBuilder.builder()
        .serverUrl(serverUrl)
        .realm("master")
        .clientId("admin-cli")
        .username(adminUsername)
        .password(adminPassword)
        .build()

    fun createUser(email: String, firstName: String, lastName: String, password: String): String {
        try {
            val userRepresentation = UserRepresentation().apply {
                this.username = email
                this.email = email
                this.isEnabled = true
                this.firstName = firstName
                this.lastName = lastName
                this.isEmailVerified = false
            }

            val credential = CredentialRepresentation().apply {
                this.type = CredentialRepresentation.PASSWORD
                this.value = password
                this.isTemporary = false
            }
            userRepresentation.credentials = listOf(credential)

            val response = keycloak
                .realm(realm)
                .users()
                .create(userRepresentation)

            if (response.status != Response.Status.CREATED.statusCode) {
                throw RuntimeException("Failed to create user in Keycloak. Status: ${response.status}")
            }

            // Extract user ID from response
            val locationPath = response.location.path
            val userId = locationPath.substring(locationPath.lastIndexOf("/") + 1)
            
            logger.info("Successfully created user in Keycloak with ID: $userId")
            return userId
            
        } catch (e: Exception) {
            logger.error("Failed to create user in Keycloak", e)
            throw RuntimeException("Failed to create user in Keycloak", e)
        }
    }
}