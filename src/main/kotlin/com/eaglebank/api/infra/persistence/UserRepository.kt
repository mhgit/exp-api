package com.eaglebank.api.infra.persistence

import com.eaglebank.api.domain.model.Address
import com.eaglebank.api.domain.model.User
import com.eaglebank.api.domain.repository.IUserRepository
import com.eaglebank.api.infra.keycloak.KeycloakService
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class UserRepository(
    private val keycloakService: KeycloakService
) : IUserRepository {
    private val logger = LoggerFactory.getLogger(UserRepository::class.java)

    override fun createUser(user: User): User = transaction {
        try {
            // Create user in Keycloak first
            val keycloakId = keycloakService.createUser(
                email = user.email,
                firstName = user.name.split(" ").firstOrNull() ?: "",
                lastName = user.name.split(" ").drop(1).joinToString(" "),
                password = "temporary-password" // You might want to generate this or handle differently
            )

            // Create user in local database
            val userEntity = UserEntity.new(user.id) {
                name = user.name
                addressLine1 = user.address.line1
                addressLine2 = user.address.line2
                addressLine3 = user.address.line3
                town = user.address.town
                county = user.address.county
                postcode = user.address.postcode
                phoneNumber = user.phoneNumber
                email = user.email
                this.keycloakId = keycloakId
                createdTimestamp = LocalDateTime.now().toString()
                updatedTimestamp = LocalDateTime.now().toString()
            }

            userEntity.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to create user", e)
            throw e
        }
    }

    override fun getUserById(id: String): User? = transaction {
        try {
            UserEntity.findById(id)?.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to get user by id: $id", e)
            null
        }
    }

    override fun updateUser(id: String, user: User): User? = transaction {
        try {
            val entity = UserEntity.findById(id) ?: return@transaction null

            // Update Keycloak user
            entity.keycloakId?.let { keycloakId ->
                keycloakService.updateUser(
                    keycloakId,
                    email = user.email,
                    firstName = user.name.split(" ").firstOrNull() ?: "",
                    lastName = user.name.split(" ").drop(1).joinToString(" ")
                )
            }

            // Update local database
            entity.apply {
                name = user.name
                addressLine1 = user.address.line1
                addressLine2 = user.address.line2
                addressLine3 = user.address.line3
                town = user.address.town
                county = user.address.county
                postcode = user.address.postcode
                phoneNumber = user.phoneNumber
                email = user.email
                updatedTimestamp = LocalDateTime.now().toString()
            }

            entity.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to update user: $id", e)
            null
        }
    }

    override fun deleteUser(id: String): Boolean = transaction {
        try {
            val entity = UserEntity.findById(id) ?: return@transaction false

            // Delete from Keycloak first
            entity.keycloakId?.let { keycloakId ->
                keycloakService.deleteUser(keycloakId)
            }

            // Delete from local database
            entity.delete()
            true
        } catch (e: Exception) {
            logger.error("Failed to delete user: $id", e)
            false
        }
    }

    override fun getAllUsers(): List<User> = transaction {
        try {
            UserEntity.all().map { it.toDomain() }
        } catch (e: Exception) {
            logger.error("Failed to get all users", e)
            emptyList()
        }
    }
}

private fun UserEntity.toDomain() = User(
    id = id.value,
    name = name,
    address = Address(
        line1 = addressLine1,
        line2 = addressLine2,
        line3 = addressLine3,
        town = town,
        county = county,
        postcode = postcode
    ),
    phoneNumber = phoneNumber,
    email = email,
    keycloakId = keycloakId,
    createdTimestamp = createdTimestamp,
    updatedTimestamp = updatedTimestamp
)