package com.eaglebank.api.infra.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
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
            // First check if user already exists
            val existingUsers = keycloak
                .realm(realm)
                .users()
                .search(email, 0, 1)

            if (existingUsers.isNotEmpty()) {
                logger.warn("User with email $email already exists in Keycloak")
                return existingUsers[0].id
            }

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

            when (response.status) {
                Response.Status.CREATED.statusCode -> {
                    val locationPath = response.location.path
                    val userId = locationPath.substring(locationPath.lastIndexOf("/") + 1)
                    logger.info("Successfully created user in Keycloak with ID: $userId")
                    return userId
                }

                Response.Status.CONFLICT.statusCode -> {
                    logger.warn("Concurrent attempt to create user with email $email")
                    // Retry getting the user in case of concurrent creation
                    val users = keycloak.realm(realm).users().search(email, 0, 1)
                    return users[0].id
                }

                else -> {
                    throw RuntimeException("Failed to create user in Keycloak. Status: ${response.status}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to create user in Keycloak", e)
            throw RuntimeException("Failed to create user in Keycloak", e)
        }
    }

    fun updateUser(userId: String, email: String, firstName: String, lastName: String) {
        try {
            val userResource = keycloak.realm(realm).users().get(userId)
            val userRepresentation = userResource.toRepresentation().apply {
                this.email = email
                this.firstName = firstName
                this.lastName = lastName
                this.username = email  // Keeping username in sync with email
            }

            userResource.update(userRepresentation)
            logger.info("Successfully updated user in Keycloak with ID: $userId")

        } catch (e: Exception) {
            logger.error("Failed to update user in Keycloak", e)
            throw RuntimeException("Failed to update user in Keycloak", e)
        }
    }

    fun deleteUser(userId: String) {
        try {
            val response = keycloak.realm(realm).users().delete(userId)
            if (response.status != Response.Status.NO_CONTENT.statusCode) {
                throw RuntimeException("Failed to delete user in Keycloak. Status: ${response.status}")
            }
            logger.info("Successfully deleted user in Keycloak with ID: $userId")

        } catch (e: Exception) {
            logger.error("Failed to delete user in Keycloak", e)
            throw RuntimeException("Failed to delete user in Keycloak", e)
        }
    }
}