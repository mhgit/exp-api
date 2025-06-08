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

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val refreshExpiresIn: Int? = null,
    val tokenType: String? = null
)