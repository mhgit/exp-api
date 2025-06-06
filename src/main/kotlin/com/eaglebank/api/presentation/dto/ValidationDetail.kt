package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class ValidationDetail(
    val field: String,
    val message: String,
    val type: String
)