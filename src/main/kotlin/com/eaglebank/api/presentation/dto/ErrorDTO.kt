package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class BadRequestErrorResponse(
    val message: String,
    val details: List<ValidationDetail>
)