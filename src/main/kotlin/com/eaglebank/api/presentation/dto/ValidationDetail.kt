package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

/**
 * Represents a detailed validation error encountered during request validation.
 * Each instance describes a specific validation issue, including the field that failed validation,
 * a human-readable message, and the type of validation failure.
 *
 * @property field The name of the field that failed validation (e.g., "email", "address.postcode")
 * @property message A human-readable description of the validation error
 * @property type The type of validation failure (see [ValidationType])
 *
 * @see ValidationType.REQUIRED_FIELD
 * @see ValidationType.INVALID_FORMAT
 *
 * Example usage:
 * ```kotlin
 * // Create a validation error for a required field
 * val requiredError = ValidationDetail.required("email")
 *
 * // Create a validation error for invalid format with additional details
 * val formatError = ValidationDetail.invalidFormat(
 *     fieldName = "phoneNumber",
 *     details = "Must be in format: +XX-XXXX-XXXX"
 * )
 * ```
 *
 * This class is serializable and can be used directly in API responses to provide
 * structured validation feedback to clients.
 */

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
