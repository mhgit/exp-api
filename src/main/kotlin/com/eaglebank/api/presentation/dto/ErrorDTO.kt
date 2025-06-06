package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String
)

@Serializable
data class BadRequestErrorResponse(
    val message: String,
    val details: List<ValidationDetail>
)