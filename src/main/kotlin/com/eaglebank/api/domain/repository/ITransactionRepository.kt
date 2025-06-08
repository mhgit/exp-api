package com.eaglebank.api.domain.repository

import com.eaglebank.api.domain.model.Transaction

/**
 * Repository interface for Transaction operations.
 */
interface ITransactionRepository {
    /**
     * Create a new transaction.
     *
     * @param transaction The transaction to create
     * @return The created transaction
     */
    fun createTransaction(transaction: Transaction): Transaction

    /**
     * Get a transaction by its ID.
     *
     * @param transactionId The ID of the transaction to retrieve
     * @return The transaction if found, null otherwise
     */
    fun getTransactionById(transactionId: String): Transaction?

    /**
     * Get a transaction by its ID and account number.
     *
     * @param accountNumber The account number
     * @param transactionId The ID of the transaction to retrieve
     * @return The transaction if found, null otherwise
     */
    fun getTransactionByIdAndAccountNumber(accountNumber: String, transactionId: String): Transaction?

    /**
     * Get all transactions for an account.
     *
     * @param accountNumber The account number
     * @return List of transactions for the account
     */
    fun getTransactionsByAccountNumber(accountNumber: String): List<Transaction>
}