package com.eaglebank.api.presentation.mapper

import com.eaglebank.api.domain.model.Transaction
import com.eaglebank.api.presentation.dto.CreateTransactionRequest
import com.eaglebank.api.presentation.dto.Currency
import com.eaglebank.api.presentation.dto.ListTransactionsResponse
import com.eaglebank.api.presentation.dto.TransactionResponse
import java.time.Instant
import java.util.*
import com.eaglebank.api.domain.model.TransactionType as DomainTransactionType
import com.eaglebank.api.presentation.dto.TransactionType as DtoTransactionType

/**
 * Mapper for converting between domain models and DTOs for transactions.
 */
object TransactionMapper {
    /**
     * Converts a CreateTransactionRequest to a Transaction domain model.
     *
     * @param request The request DTO
     * @param accountNumber The account number associated with this transaction
     * @param userId The ID of the user who owns the account
     * @return A Transaction domain model
     */
    fun createRequestToDomain(request: CreateTransactionRequest, accountNumber: String, userId: String): Transaction {
        val now = Instant.now()
        val amount = when (request.type) {
            DtoTransactionType.deposit -> request.amount
            DtoTransactionType.withdrawal -> -request.amount // Negative amount for withdrawals
        }

        return Transaction(
            id = "txn-${UUID.randomUUID()}",
            accountNumber = accountNumber,
            userId = userId,
            amount = amount,
            currency = request.currency.name,
            type = mapTransactionType(request.type),
            reference = request.reference,
            createdTimestamp = now
        )
    }

    /**
     * Converts a Transaction domain model to a TransactionResponse DTO.
     *
     * @param transaction The domain model
     * @return A TransactionResponse DTO
     */
    fun toTransactionResponse(transaction: Transaction): TransactionResponse {
        val displayAmount = Math.abs(transaction.amount) // Always display positive amount

        return TransactionResponse(
            id = transaction.id,
            amount = displayAmount,
            currency = Currency.valueOf(transaction.currency),
            type = mapTransactionType(transaction.type),
            reference = transaction.reference,
            userId = transaction.userId,
            createdTimestamp = transaction.createdTimestamp.toString()
        )
    }

    /**
     * Converts a list of Transaction domain models to a ListTransactionsResponse DTO.
     *
     * @param transactions The list of domain models
     * @return A ListTransactionsResponse DTO
     */
    fun toListTransactionsResponse(transactions: List<Transaction>): ListTransactionsResponse {
        return ListTransactionsResponse(
            transactions = transactions.map { toTransactionResponse(it) }
        )
    }

    /**
     * Maps a DTO TransactionType to a domain TransactionType.
     *
     * @param type The DTO transaction type
     * @return The domain transaction type
     */
    private fun mapTransactionType(type: DtoTransactionType): DomainTransactionType {
        return when (type) {
            DtoTransactionType.deposit -> DomainTransactionType.DEPOSIT
            DtoTransactionType.withdrawal -> DomainTransactionType.WITHDRAWAL
        }
    }

    /**
     * Maps a domain TransactionType to a DTO TransactionType.
     *
     * @param type The domain transaction type
     * @return The DTO transaction type
     */
    private fun mapTransactionType(type: DomainTransactionType): DtoTransactionType {
        return when (type) {
            DomainTransactionType.DEPOSIT -> DtoTransactionType.deposit
            DomainTransactionType.WITHDRAWAL -> DtoTransactionType.withdrawal
        }
    }
}