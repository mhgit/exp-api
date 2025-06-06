package com.eaglebank.api.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateBankAccountRequest(
    val name: String,
    val accountType: AccountType
)

@Serializable
data class UpdateBankAccountRequest(
    val name: String? = null,
    val accountType: AccountType? = null
)

@Serializable
data class BankAccountResponse(
    val accountNumber: String,
    val sortCode: String,
    val name: String,
    val accountType: AccountType,
    val balance: Double,
    val currency: Currency,
    val createdTimestamp: String,
    val updatedTimestamp: String
)

@Serializable
data class ListBankAccountsResponse(
    val accounts: List<BankAccountResponse>
)

@Serializable
enum class AccountType {
    personal
}

@Serializable
enum class Currency {
    GBP
}