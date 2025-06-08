package com.eaglebank.api.domain.model

import java.time.Instant

/**
 * Represents a bank account in the domain model.
 *
 * @property id The unique identifier for the bank account
 * @property userId The ID of the user who owns this account
 * @property accountNumber The account number
 * @property sortCode The sort code for the bank account
 * @property name The name of the account
 * @property accountType The type of account (e.g., personal)
 * @property balance The current balance of the account
 * @property currency The currency of the account
 * @property createdTimestamp When the account was created
 * @property updatedTimestamp When the account was last updated
 */
data class BankAccount(
    val id: String,
    val userId: String,
    val accountNumber: String,
    val sortCode: String,
    val name: String,
    val accountType: String,
    val balance: Double,
    val currency: String,
    val createdTimestamp: Instant,
    val updatedTimestamp: Instant
)