package com.eaglebank.api.infra.persistence

import com.eaglebank.api.presentation.dto.Address
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UpdateUserRequest
import com.eaglebank.api.presentation.dto.UserResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class UserRepository {

    fun createUser(request: CreateUserRequest): UserResponse = transaction {
        val now = Instant.now()
        val userEntity = UserEntity.new {
            name = request.name
            addressLine1 = request.address.line1
            addressLine2 = request.address.line2
            addressLine3 = request.address.line3
            town = request.address.town
            county = request.address.county
            postcode = request.address.postcode
            phoneNumber = request.phoneNumber
            email = request.email
            createdTimestamp = now
            updatedTimestamp = now
        }
        userEntity.toUserResponse()
    }

    fun getUserById(id: UUID): UserResponse? = transaction {
        UserEntity.findById(id)?.toUserResponse()
    }

    fun updateUser(id: UUID, request: UpdateUserRequest): UserResponse? = transaction {
        UserEntity.findById(id)?.apply {
            request.name?.let { this.name = it }
            request.phoneNumber?.let { this.phoneNumber = it }
            request.email?.let { this.email = it }
            request.address?.let {
                this.addressLine1 = it.line1
                this.addressLine2 = it.line2
                this.addressLine3 = it.line3
                this.town = it.town
                this.county = it.county
                this.postcode = it.postcode
            }
            updatedTimestamp = Instant.now()
        }?.toUserResponse()
    }

    fun deleteUser(id: UUID): Boolean = transaction {
        val deletedRows = UserTable.deleteWhere { UserTable.id eq id }
        deletedRows > 0
    }

    fun getAllUsers(): List<UserResponse> = transaction {
        UserEntity.all().map { it.toUserResponse() }
    }

    // Extension function to convert UserEntity to UserResponse
    private fun UserEntity.toUserResponse(): UserResponse {
        return UserResponse(
            id = this.id.value.toString(),
            name = this.name,
            address = Address(
                line1 = this.addressLine1,
                line2 = this.addressLine2,
                line3 = this.addressLine3,
                town = this.town,
                county = this.county,
                postcode = this.postcode
            ),
            phoneNumber = this.phoneNumber,
            email = this.email,
            createdTimestamp = this.createdTimestamp.toString(),
            updatedTimestamp = this.updatedTimestamp.toString()
        )
    }
}