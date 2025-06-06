package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val line1: String,
    val line2: String? = null,
    val line3: String? = null,
    val town: String,
    val county: String,
    val postcode: String
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val address: Address,
    val phoneNumber: String,
    val email: String
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val address: Address? = null,
    val phoneNumber: String? = null,
    val email: String? = null
)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val address: Address,
    val phoneNumber: String,
    val email: String,
    val createdTimestamp: String,
    val updatedTimestamp: String
)