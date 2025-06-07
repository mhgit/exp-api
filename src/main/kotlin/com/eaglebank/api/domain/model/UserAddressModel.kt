package com.eaglebank.api.domain.model

data class Address(
    val line1: String,
    val line2: String?,
    val line3: String?,
    val town: String,
    val county: String,
    val postcode: String
)

data class User(
    val id: String,
    val name: String,
    val address: Address,
    val phoneNumber: String,
    val email: String,
    val keycloakId: String?,
    val createdTimestamp: String,
    val updatedTimestamp: String
) {
    companion object {
        fun generateId(): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            val randomPart = (1..10)
                .map { allowedChars.random() }
                .joinToString("")
            return "usr-$randomPart"
        }
    }
}