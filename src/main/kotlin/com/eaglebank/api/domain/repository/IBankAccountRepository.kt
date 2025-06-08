package com.eaglebank.api.domain.repository

import com.eaglebank.api.domain.model.BankAccount

/**
 * Repository interface for bank account operations.
 */
interface IBankAccountRepository {
    /**
     * Creates a new bank account.
     *
     * @param bankAccount The bank account to create
     * @return The created bank account with generated ID and timestamps
     */
    fun createBankAccount(bankAccount: BankAccount): BankAccount

    /**
     * Gets a bank account by account number.
     *
     * @param accountNumber The account number to look up
     * @return The bank account if found, null otherwise
     */
    fun getBankAccountByAccountNumber(accountNumber: String): BankAccount?

    /**
     * Updates a bank account.
     *
     * @param accountNumber The account number of the account to update
     * @param bankAccount The updated bank account data
     * @return The updated bank account if successful, null otherwise
     */
    fun updateBankAccount(accountNumber: String, bankAccount: BankAccount): BankAccount?

    /**
     * Deletes a bank account.
     *
     * @param accountNumber The account number of the account to delete
     * @return true if the account was deleted, false otherwise
     */
    fun deleteBankAccount(accountNumber: String): Boolean

    /**
     * Gets all bank accounts for a user.
     *
     * @param userId The ID of the user
     * @return List of bank accounts owned by the user
     */
    fun getBankAccountsByUserId(userId: String): List<BankAccount>

    /**
     * Gets all bank accounts.
     *
     * @return List of all bank accounts
     */
    fun getAllBankAccounts(): List<BankAccount>
}