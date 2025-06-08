package com.eaglebank.api.domain.model

import java.time.Instant

/**
 * Represents a transaction in the domain model.
 *
 * @property id The unique identifier for the transaction
 * @property accountNumber The account number associated with this transaction
 * @property userId The ID of the user who owns the account
 * @property amount The amount of the transaction (positive for deposits, negative for withdrawals)
 * @property currency The currency of the transaction
 * @property type The type of transaction (deposit or withdrawal)
 * @property reference Optional reference for the transaction
 * @property createdTimestamp When the transaction was created
 */
data class Transaction(
    val id: String,
    val accountNumber: String,
    val userId: String,
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val reference: String? = null,
    val createdTimestamp: Instant
)

/**
 * Enum representing the type of transaction.
 */
enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL
}