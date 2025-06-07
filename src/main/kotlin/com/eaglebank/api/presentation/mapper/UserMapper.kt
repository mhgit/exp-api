package com.eaglebank.api.presentation.mapper

import com.eaglebank.api.domain.model.Address
import com.eaglebank.api.domain.model.User
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UpdateUserRequest
import com.eaglebank.api.presentation.dto.UserResponse

object UserMapper {
    fun toUserResponse(user: User): UserResponse = UserResponse(
        id = user.id,
        name = user.name,
        address = com.eaglebank.api.presentation.dto.Address(
            line1 = user.address.line1,
            line2 = user.address.line2,
            line3 = user.address.line3,
            town = user.address.town,
            county = user.address.county,
            postcode = user.address.postcode
        ),
        phoneNumber = user.phoneNumber,
        email = user.email,
        createdTimestamp = user.createdTimestamp,
        updatedTimestamp = user.updatedTimestamp
    )

    fun createRequestToDomain(request: CreateUserRequest): User = User(
        id = User.generateId(),
        name = request.name,
        address = Address(
            line1 = request.address.line1,
            line2 = request.address.line2,
            line3 = request.address.line3,
            town = request.address.town,
            county = request.address.county,
            postcode = request.address.postcode
        ),
        phoneNumber = request.phoneNumber,
        email = request.email,
        keycloakId = null,
        createdTimestamp = "",  // Will be set in repository
        updatedTimestamp = ""   // Will be set in repository
    )

    fun updateRequestToDomain(id: String, existing: User, request: UpdateUserRequest): User = User(
        id = id,
        name = request.name ?: existing.name,
        address = Address(
            line1 = request.address?.line1 ?: existing.address.line1,
            line2 = request.address?.line2 ?: existing.address.line2,
            line3 = request.address?.line3 ?: existing.address.line3,
            town = request.address?.town ?: existing.address.town,
            county = request.address?.county ?: existing.address.county,
            postcode = request.address?.postcode ?: existing.address.postcode
        ),
        phoneNumber = request.phoneNumber ?: existing.phoneNumber,
        email = request.email ?: existing.email,
        keycloakId = existing.keycloakId,
        createdTimestamp = existing.createdTimestamp,
        updatedTimestamp = existing.updatedTimestamp
    )
}