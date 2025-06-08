package com.eaglebank.api.infra.persistence

import com.eaglebank.api.domain.model.Transaction
import com.eaglebank.api.domain.model.TransactionType
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionRepositoryTest {
    private lateinit var transactionRepository: TransactionRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testTransaction = Transaction(
        id = "txn-test123456",
        accountNumber = "12345678",
        userId = "usr-test123456",
        amount = 100.0,
        currency = "GBP",
        type = TransactionType.DEPOSIT,
        reference = "Test deposit",
        createdTimestamp = Instant.now()
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
            SchemaUtils.create(TransactionTable)
        }

        transactionRepository = TransactionRepository()
    }

    @AfterEach
    fun tearDown() {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        Dispatchers.resetMain()

        transaction {
            SchemaUtils.drop(TransactionTable)
        }
    }

    @Test
    fun `createTransaction should successfully create transaction`() = runTest {
        // Given
        val transactionToCreate = testTransaction.copy(
            id = "txn-" + System.currentTimeMillis()
        )

        // When
        val createdTransaction = transactionRepository.createTransaction(transactionToCreate)

        // Then
        assertNotNull(createdTransaction)
        assertEquals(transactionToCreate.id, createdTransaction.id)
        assertEquals(transactionToCreate.accountNumber, createdTransaction.accountNumber)
        assertEquals(transactionToCreate.amount, createdTransaction.amount)
        assertEquals(transactionToCreate.type, createdTransaction.type)
    }

    @Test
    fun `getTransactionById should return transaction when exists`() = runBlocking {
        // Given
        // First create a transaction
        val createdTransaction = transactionRepository.createTransaction(
            testTransaction.copy(
                id = "txn-" + System.currentTimeMillis()
            )
        )

        // When
        val retrievedTransaction = transactionRepository.getTransactionById(createdTransaction.id)

        // Then
        assertNotNull(retrievedTransaction)
        assertEquals(createdTransaction.id, retrievedTransaction?.id)
        assertEquals(createdTransaction.accountNumber, retrievedTransaction?.accountNumber)
        assertEquals(createdTransaction.amount, retrievedTransaction?.amount)
    }

    @Test
    fun `getTransactionById should return null when transaction doesn't exist`() = runBlocking {
        // When
        val retrievedTransaction = transactionRepository.getTransactionById("txn-nonexistent")

        // Then
        assertNull(retrievedTransaction)
    }

    @Test
    fun `getTransactionByIdAndAccountNumber should return transaction when exists`() = runBlocking {
        // Given
        // First create a transaction
        val createdTransaction = transactionRepository.createTransaction(
            testTransaction.copy(
                id = "txn-" + System.currentTimeMillis()
            )
        )

        // When
        val retrievedTransaction = transactionRepository.getTransactionByIdAndAccountNumber(
            createdTransaction.accountNumber,
            createdTransaction.id
        )

        // Then
        assertNotNull(retrievedTransaction)
        assertEquals(createdTransaction.id, retrievedTransaction?.id)
        assertEquals(createdTransaction.accountNumber, retrievedTransaction?.accountNumber)
    }

    @Test
    fun `getTransactionByIdAndAccountNumber should return null when transaction doesn't exist`() = runBlocking {
        // When
        val retrievedTransaction = transactionRepository.getTransactionByIdAndAccountNumber(
            "nonexistent",
            "txn-nonexistent"
        )

        // Then
        assertNull(retrievedTransaction)
    }

    @Test
    fun `getTransactionsByAccountNumber should return transactions for account`() = runBlocking {
        // Given
        val accountNumber = "12345678"
        val transaction1 = transactionRepository.createTransaction(
            testTransaction.copy(
                id = "txn-" + System.currentTimeMillis(),
                accountNumber = accountNumber,
                type = TransactionType.DEPOSIT
            )
        )
        val transaction2 = transactionRepository.createTransaction(
            testTransaction.copy(
                id = "txn-" + System.currentTimeMillis() + 1,
                accountNumber = accountNumber,
                type = TransactionType.WITHDRAWAL,
                amount = -50.0
            )
        )

        // When
        val transactions = transactionRepository.getTransactionsByAccountNumber(accountNumber)

        // Then
        assertEquals(2, transactions.size)
        assertTrue(transactions.any { it.id == transaction1.id })
        assertTrue(transactions.any { it.id == transaction2.id })
    }

    @Test
    fun `getTransactionsByAccountNumber should return empty list for non-existent account`() = runBlocking {
        // When
        val transactions = transactionRepository.getTransactionsByAccountNumber("nonexistent")

        // Then
        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `should create and retrieve multiple transactions for an account`() = runBlocking {
        // Given
        val accountNumber = "98765432"
        val transactionIds = mutableListOf<String>()
        val transactionCount = 5

        // When - Create 5 transactions
        for (i in 1..transactionCount) {
            val transaction = transactionRepository.createTransaction(
                testTransaction.copy(
                    id = "txn-multi-${System.currentTimeMillis()}-$i",
                    accountNumber = accountNumber,
                    amount = i * 100.0,
                    reference = "Test transaction $i"
                )
            )
            transactionIds.add(transaction.id)
        }

        // Then - Retrieve and verify all transactions
        val retrievedTransactions = transactionRepository.getTransactionsByAccountNumber(accountNumber)

        // Verify count
        assertEquals(
            transactionCount,
            retrievedTransactions.size,
            "Should retrieve exactly $transactionCount transactions"
        )

        // Verify all transaction IDs are present
        transactionIds.forEach { id ->
            assertTrue(retrievedTransactions.any { it.id == id }, "Transaction with ID $id should be retrieved")
        }

        // Verify transaction details
        retrievedTransactions.forEachIndexed { index, transaction ->
            val expectedId = transactionIds[index]
            val retrievedTransaction = retrievedTransactions.find { it.id == expectedId }

            assertNotNull(retrievedTransaction, "Transaction with ID $expectedId should exist")
            assertEquals(accountNumber, retrievedTransaction?.accountNumber, "Account number should match")
            assertEquals(testTransaction.type, retrievedTransaction?.type, "Transaction type should match")
        }
    }
}
