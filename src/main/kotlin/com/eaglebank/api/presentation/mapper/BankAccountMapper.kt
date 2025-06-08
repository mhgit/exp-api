package com.eaglebank.api.presentation.mapper

import com.eaglebank.api.domain.model.BankAccount
import com.eaglebank.api.presentation.dto.*
import com.eaglebank.api.presentation.dto.Currency
import java.time.Instant
import java.util.*

/**
 * Mapper for converting between domain models and DTOs for bank accounts.
 */
object BankAccountMapper {
    /**
     * Converts a CreateBankAccountRequest to a BankAccount domain model.
     *
     * @param request The request DTO
     * @param userId The ID of the user who owns the account
     * @return A BankAccount domain model
     */
    fun createRequestToDomain(request: CreateBankAccountRequest, userId: String): BankAccount {
        val now = Instant.now()
        val accountNumber = generateAccountNumber()
        val sortCode = "12-34-56" // Default sort code, would be configurable in a real system

        return BankAccount(
            id = "acc-${UUID.randomUUID()}",
            userId = userId,
            accountNumber = accountNumber,
            sortCode = sortCode,
            name = request.name,
            accountType = request.accountType.name,
            balance = 0.0, // New accounts start with zero balance
            currency = Currency.GBP.name, // Default currency
            createdTimestamp = now,
            updatedTimestamp = now
        )
    }

    /**
     * Converts a BankAccount domain model to a BankAccountResponse DTO.
     *
     * @param bankAccount The domain model
     * @return A BankAccountResponse DTO
     */
    fun toBankAccountResponse(bankAccount: BankAccount): BankAccountResponse {
        return BankAccountResponse(
            accountNumber = bankAccount.accountNumber,
            sortCode = bankAccount.sortCode,
            name = bankAccount.name,
            accountType = AccountType.valueOf(bankAccount.accountType),
            balance = bankAccount.balance,
            currency = Currency.valueOf(bankAccount.currency),
            createdTimestamp = bankAccount.createdTimestamp.toString(),
            updatedTimestamp = bankAccount.updatedTimestamp.toString()
        )
    }

    /**
     * Updates a BankAccount domain model with values from an UpdateBankAccountRequest.
     *
     * @param existingAccount The existing account to update
     * @param request The update request
     * @return An updated BankAccount domain model
     */
    fun updateRequestToDomain(existingAccount: BankAccount, request: UpdateBankAccountRequest): BankAccount {
        return existingAccount.copy(
            name = request.name ?: existingAccount.name,
            accountType = request.accountType?.name ?: existingAccount.accountType,
            updatedTimestamp = Instant.now()
        )
    }

    /**
     * Generates a random account number.
     *
     * @return A random account number in the format "01XXXXXX"
     */
    private fun generateAccountNumber(): String {
        val randomPart = (100000..999999).random()
        return "01$randomPart"
    }
}