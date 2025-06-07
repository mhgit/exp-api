package com.eaglebank.api.infra.persistence

import com.eaglebank.api.domain.model.Address
import com.eaglebank.api.domain.model.User
import com.eaglebank.api.domain.repository.IUserRepository
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class UserRepository : IUserRepository {
    private fun toEntity(user: User): UserEntity {
        return transaction {
            UserEntity.new(user.id) {
                name = user.name
                addressLine1 = user.address.line1
                addressLine2 = user.address.line2
                addressLine3 = user.address.line3
                town = user.address.town
                county = user.address.county
                postcode = user.address.postcode
                phoneNumber = user.phoneNumber
                email = user.email
                keycloakId = user.keycloakId
                createdTimestamp = user.createdTimestamp
                updatedTimestamp = user.updatedTimestamp
            }
        }
    }

    private fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id.value,
            name = entity.name,
            address = Address(
                line1 = entity.addressLine1,
                line2 = entity.addressLine2,
                line3 = entity.addressLine3,
                town = entity.town,
                county = entity.county,
                postcode = entity.postcode
            ),
            phoneNumber = entity.phoneNumber,
            email = entity.email,
            keycloakId = entity.keycloakId,
            createdTimestamp = entity.createdTimestamp,
            updatedTimestamp = entity.updatedTimestamp
        )
    }

    override fun createUser(user: User): User = transaction {
        val timestamp = Instant.now().toString()
        val userToSave = user.copy(
            createdTimestamp = timestamp,
            updatedTimestamp = timestamp
        )
        toDomain(toEntity(userToSave))
    }

    override fun getUserById(id: String): User? = transaction {
        UserEntity.findById(id)?.let { toDomain(it) }
    }

    override fun updateUser(id: String, user: User): User? = transaction {
        UserEntity.findById(id)?.apply {
            name = user.name
            addressLine1 = user.address.line1
            addressLine2 = user.address.line2
            addressLine3 = user.address.line3
            town = user.address.town
            county = user.address.county
            postcode = user.address.postcode
            phoneNumber = user.phoneNumber
            email = user.email
            keycloakId = user.keycloakId
            updatedTimestamp = Instant.now().toString()
        }?.let { toDomain(it) }
    }

    override fun deleteUser(id: String): Boolean = transaction {
        UserEntity.findById(id)?.let {
            it.delete()
            true
        } ?: false
    }

    override fun getAllUsers(): List<User> = transaction {
        UserEntity.all().map { toDomain(it) }
    }
}