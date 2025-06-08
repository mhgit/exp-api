package com.eaglebank.api.infra.persistence

import com.eaglebank.api.domain.model.Transaction
import com.eaglebank.api.domain.model.TransactionType
import com.eaglebank.api.domain.repository.ITransactionRepository
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Implementation of the transaction repository.
 */
class TransactionRepository : ITransactionRepository {
    private val logger = LoggerFactory.getLogger(TransactionRepository::class.java)

    override fun createTransaction(transaction: Transaction): Transaction = transaction {
        try {
            val entity = TransactionEntity.new(transaction.id) {
                accountNumber = transaction.accountNumber
                userId = transaction.userId
                amount = transaction.amount
                currency = transaction.currency
                type = transaction.type.name
                reference = transaction.reference
                createdTimestamp = transaction.createdTimestamp.toString()
            }

            entity.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to create transaction", e)
            throw e
        }
    }

    override fun getTransactionById(transactionId: String): Transaction? = transaction {
        try {
            TransactionEntity.findById(transactionId)?.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to get transaction by ID: $transactionId", e)
            null
        }
    }

    override fun getTransactionByIdAndAccountNumber(accountNumber: String, transactionId: String): Transaction? =
        transaction {
            try {
                TransactionEntity.find {
                    (TransactionTable.id eq transactionId) and (TransactionTable.accountNumber eq accountNumber)
                }.firstOrNull()?.toDomain()
            } catch (e: Exception) {
                logger.error("Failed to get transaction by ID: $transactionId and account number: $accountNumber", e)
                null
            }
        }

    override fun getTransactionsByAccountNumber(accountNumber: String): List<Transaction> = transaction {
        try {
            TransactionEntity.find { TransactionTable.accountNumber eq accountNumber }
                .map { it.toDomain() }
        } catch (e: Exception) {
            logger.error("Failed to get transactions by account number: $accountNumber", e)
            emptyList()
        }
    }
}

/**
 * Extension function to convert a TransactionEntity to a Transaction domain model.
 */
private fun TransactionEntity.toDomain() = Transaction(
    id = id.value,
    accountNumber = accountNumber,
    userId = userId,
    amount = amount,
    currency = currency,
    type = TransactionType.valueOf(type),
    reference = reference,
    createdTimestamp = Instant.parse(createdTimestamp)
)