package com.eaglebank.api.infra.persistence

import com.eaglebank.api.domain.model.BankAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.*

class BankAccountRepositoryTest {
    private lateinit var bankAccountRepository: BankAccountRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testBankAccount = BankAccount(
        id = "acc-test123456",
        userId = "usr-test123456",
        accountNumber = "12345678",
        sortCode = "123456",
        name = "Test Account",
        accountType = "Current",
        balance = 1000.0,
        currency = "GBP",
        createdTimestamp = Instant.now(),
        updatedTimestamp = Instant.now()
    )

    @BeforeEach
    fun setup() {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        Dispatchers.setMain(testDispatcher)

        // Set up in-memory H2 database
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        // Create tables
        transaction {
            SchemaUtils.create(BankAccountTable)
        }

        bankAccountRepository = BankAccountRepository()
    }

    @AfterEach
    fun tearDown() {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        Dispatchers.resetMain()

        transaction {
            SchemaUtils.drop(BankAccountTable)
        }
    }

    @Test
    fun `createBankAccount should successfully create bank account`() = runTest {
        // Given
        val accountToCreate = testBankAccount.copy(
            id = "acc-" + System.currentTimeMillis(),
            accountNumber = "12345678"
        )

        // When
        val createdAccount = bankAccountRepository.createBankAccount(accountToCreate)

        // Then
        assertNotNull(createdAccount)
        assertEquals(accountToCreate.id, createdAccount.id)
        assertEquals(accountToCreate.accountNumber, createdAccount.accountNumber)
        assertEquals(accountToCreate.name, createdAccount.name)
    }

    @Test
    fun `getBankAccountByAccountNumber should return account when exists`() = runBlocking {
        // Given
        // First create an account
        val createdAccount = bankAccountRepository.createBankAccount(
            testBankAccount.copy(
                id = "acc-" + System.currentTimeMillis(),
                accountNumber = "12345678"
            )
        )

        // When
        val retrievedAccount = bankAccountRepository.getBankAccountByAccountNumber(createdAccount.accountNumber)

        // Then
        assertNotNull(retrievedAccount)
        assertEquals(createdAccount.id, retrievedAccount?.id)
        assertEquals(createdAccount.accountNumber, retrievedAccount?.accountNumber)
    }

    @Test
    fun `getBankAccountByAccountNumber should return null when account doesn't exist`() = runBlocking {
        // When
        val retrievedAccount = bankAccountRepository.getBankAccountByAccountNumber("nonexistent")

        // Then
        assertNull(retrievedAccount)
    }

    @Test
    fun `updateBankAccount should successfully update existing account`() = runBlocking {
        // Given
        val createdAccount = bankAccountRepository.createBankAccount(
            testBankAccount.copy(
                id = "acc-" + System.currentTimeMillis(),
                accountNumber = "12345678"
            )
        )
        val updatedName = "Updated Test Account"
        val updatedType = "Savings"

        val accountToUpdate = createdAccount.copy(
            name = updatedName,
            accountType = updatedType
        )

        // When
        val updatedAccount = bankAccountRepository.updateBankAccount(createdAccount.accountNumber, accountToUpdate)

        // Then
        assertNotNull(updatedAccount)
        assertEquals(updatedName, updatedAccount?.name)
        assertEquals(updatedType, updatedAccount?.accountType)
    }

    @Test
    fun `updateBankAccount should return null for non-existent account`() = runBlocking {
        // When
        val updatedAccount = bankAccountRepository.updateBankAccount("nonexistent", testBankAccount)

        // Then
        assertNull(updatedAccount)
    }

    @Test
    fun `deleteBankAccount should return true when account exists`() = runBlocking {
        // Given
        val createdAccount = bankAccountRepository.createBankAccount(
            testBankAccount.copy(
                id = "acc-" + System.currentTimeMillis(),
                accountNumber = "12345678"
            )
        )

        // When
        val result = bankAccountRepository.deleteBankAccount(createdAccount.accountNumber)

        // Then
        assertTrue(result)
        assertNull(bankAccountRepository.getBankAccountByAccountNumber(createdAccount.accountNumber))
    }

    @Test
    fun `deleteBankAccount should return false when account doesn't exist`() = runBlocking {
        // When
        val result = bankAccountRepository.deleteBankAccount("nonexistent")

        // Then
        assertFalse(result)
    }

    @Test
    fun `getBankAccountsByUserId should return accounts for user`() = runBlocking {
        // Given
        val userId = "usr-test123456"
        val account1 = bankAccountRepository.createBankAccount(
            testBankAccount.copy(
                id = "acc-" + System.currentTimeMillis(),
                userId = userId,
                accountNumber = "12345678"
            )
        )
        val account2 = bankAccountRepository.createBankAccount(
            testBankAccount.copy(
                id = "acc-" + System.currentTimeMillis() + 1,
                userId = userId,
                accountNumber = "87654321"
            )
        )

        // When
        val accounts = bankAccountRepository.getBankAccountsByUserId(userId)

        // Then
        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.id == account1.id })
        assertTrue(accounts.any { it.id == account2.id })
    }

    @Test
    fun `getBankAccountsByUserId should return empty list for non-existent user`() = runBlocking {
        // When
        val accounts = bankAccountRepository.getBankAccountsByUserId("nonexistent")

        // Then
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun `getAllBankAccounts should return all accounts`() = runBlocking {
        // Given
        val account1 = bankAccountRepository.createBankAccount(
            testBankAccount.copy(
                id = "acc-" + System.currentTimeMillis(),
                accountNumber = "12345678"
            )
        )
        val account2 = bankAccountRepository.createBankAccount(
            testBankAccount.copy(
                id = "acc-" + System.currentTimeMillis() + 1,
                accountNumber = "87654321"
            )
        )

        // When
        val allAccounts = bankAccountRepository.getAllBankAccounts()

        // Then
        assertTrue(allAccounts.size >= 2)
        assertTrue(allAccounts.any { it.id == account1.id })
        assertTrue(allAccounts.any { it.id == account2.id })
    }
}