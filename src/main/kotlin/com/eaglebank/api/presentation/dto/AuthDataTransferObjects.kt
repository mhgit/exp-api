package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val accessToken: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String
)