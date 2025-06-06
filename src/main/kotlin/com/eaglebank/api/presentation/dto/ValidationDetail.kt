package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class ValidationDetail(
    val field: String,
    val message: String,
    val type: String
) {
    companion object {
        fun required(fieldName: String) = ValidationDetail(
            field = fieldName,
            message = "$fieldName cannot be empty",
            type = ValidationType.REQUIRED_FIELD.name
        )

        fun invalidFormat(fieldName: String, details: String = "") = ValidationDetail(
            field = fieldName,
            message = "Invalid $fieldName format${if (details.isNotEmpty()) ". $details" else ""}",
            type = ValidationType.INVALID_FORMAT.name
        )
    }
}
