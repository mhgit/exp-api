package com.eaglebank.api.infra.persistence

import com.eaglebank.api.domain.model.BankAccount
import com.eaglebank.api.domain.repository.IBankAccountRepository
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Implementation of the bank account repository.
 */
class BankAccountRepository : IBankAccountRepository {
    private val logger = LoggerFactory.getLogger(BankAccountRepository::class.java)

    override fun createBankAccount(bankAccount: BankAccount): BankAccount = transaction {
        try {
            val entity = BankAccountEntity.new(bankAccount.id) {
                userId = bankAccount.userId
                accountNumber = bankAccount.accountNumber
                sortCode = bankAccount.sortCode
                name = bankAccount.name
                accountType = bankAccount.accountType
                balance = bankAccount.balance
                currency = bankAccount.currency
                createdTimestamp = bankAccount.createdTimestamp.toString()
                updatedTimestamp = bankAccount.updatedTimestamp.toString()
            }

            entity.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to create bank account", e)
            throw e
        }
    }

    override fun getBankAccountByAccountNumber(accountNumber: String): BankAccount? = transaction {
        try {
            BankAccountEntity.find { BankAccountTable.accountNumber eq accountNumber }
                .firstOrNull()
                ?.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to get bank account by account number: $accountNumber", e)
            null
        }
    }

    override fun updateBankAccount(accountNumber: String, bankAccount: BankAccount): BankAccount? = transaction {
        try {
            val entity = BankAccountEntity.find { BankAccountTable.accountNumber eq accountNumber }
                .firstOrNull() ?: return@transaction null

            entity.apply {
                name = bankAccount.name
                accountType = bankAccount.accountType
                updatedTimestamp = Instant.now().toString()
            }

            entity.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to update bank account: $accountNumber", e)
            null
        }
    }

    override fun deleteBankAccount(accountNumber: String): Boolean = transaction {
        try {
            val entity = BankAccountEntity.find { BankAccountTable.accountNumber eq accountNumber }
                .firstOrNull() ?: return@transaction false

            entity.delete()
            true
        } catch (e: Exception) {
            logger.error("Failed to delete bank account: $accountNumber", e)
            false
        }
    }

    override fun getBankAccountsByUserId(userId: String): List<BankAccount> = transaction {
        try {
            BankAccountEntity.find { BankAccountTable.userId eq userId }
                .map { it.toDomain() }
        } catch (e: Exception) {
            logger.error("Failed to get bank accounts by user ID: $userId", e)
            emptyList()
        }
    }

    override fun getAllBankAccounts(): List<BankAccount> = transaction {
        try {
            BankAccountEntity.all().map { it.toDomain() }
        } catch (e: Exception) {
            logger.error("Failed to get all bank accounts", e)
            emptyList()
        }
    }
}

/**
 * Extension function to convert a BankAccountEntity to a BankAccount domain model.
 */
private fun BankAccountEntity.toDomain() = BankAccount(
    id = id.value,
    userId = userId,
    accountNumber = accountNumber,
    sortCode = sortCode,
    name = name,
    accountType = accountType,
    balance = balance,
    currency = currency,
    createdTimestamp = Instant.parse(createdTimestamp),
    updatedTimestamp = Instant.parse(updatedTimestamp)
)