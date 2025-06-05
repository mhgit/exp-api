package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTransactionRequest(
    val amount: Double,
    val currency: Currency,
    val type: TransactionType,
    val reference: String? = null
)

@Serializable
data class TransactionResponse(
    val id: String,
    val amount: Double,
    val currency: Currency,
    val type: TransactionType,
    val reference: String? = null,
    val userId: String? = null,
    val createdTimestamp: String
)

@Serializable
data class ListTransactionsResponse(
    val transactions: List<TransactionResponse>
)

@Serializable
enum class TransactionType {
    deposit,
    withdrawal
}