package com.eaglebank.api.presentation.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.eaglebank.api.domain.model.BankAccount
import com.eaglebank.api.domain.model.Transaction
import com.eaglebank.api.domain.model.TransactionType
import com.eaglebank.api.domain.repository.IBankAccountRepository
import com.eaglebank.api.domain.repository.ITransactionRepository
import com.eaglebank.api.infra.validation.TransactionRequestValidationService
import com.eaglebank.api.presentation.dto.CreateTransactionRequest
import com.eaglebank.api.presentation.dto.ValidationDetail
import com.eaglebank.api.presentation.route.transactionsRoute
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.time.Instant
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Test suite for the TransactionsRoute")
class TransactionsRouteTest : KoinTest {
    private val TRANSACTIONS_ENDPOINT = "/v1/accounts"

    // Mock JWT token for testing with required claims
    private val testToken = JWT.create()
        .withIssuer("http://localhost:8082/auth/realms/eagle-bank")
        .withSubject("test-user")
        .withClaim("email", "test@example.com")
        .sign(Algorithm.none())

    // Test dependencies
    private val testBankAccountRepository = TestBankAccountRepository()
    private val testTransactionRepository = TestTransactionRepository()
    private val testValidationService = TestTransactionRequestValidationService()

    @BeforeTest
    fun setup() {
        stopKoin() // Stop any existing Koin instance
        startKoin {
            modules(module {
                single<IBankAccountRepository> { testBankAccountRepository }
                single<ITransactionRepository> { testTransactionRepository }
                single<TransactionRequestValidationService> { testValidationService }
            })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun Application.configureTestingModule() {
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(
                    JWT.require(Algorithm.none())
                        .withIssuer("http://localhost:8082/auth/realms/eagle-bank")
                        .build()
                )
                validate { credential ->
                    // For testing purposes, we'll accept all tokens with a valid email
                    if (credential.payload.getClaim("email").asString().isNotEmpty()) {
                        JWTPrincipal(credential.payload)
                    } else null
                }
            }
        }

        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }

        routing {
            transactionsRoute()
        }
    }

    // Simple test implementation of IBankAccountRepository
    class TestBankAccountRepository : IBankAccountRepository {
        private val accounts = mutableMapOf<String, BankAccount>()

        init {
            // Add a test account for user "test-user"
            val testAccount = BankAccount(
                id = "acc-test123456",
                userId = "test-user",
                accountNumber = "01123456",
                sortCode = "12-34-56",
                name = "Test Account",
                accountType = "personal",
                balance = 1000.0,
                currency = "GBP",
                createdTimestamp = Instant.now(),
                updatedTimestamp = Instant.now()
            )
            accounts[testAccount.accountNumber] = testAccount

            // Add a test account for another user
            val otherUserAccount = BankAccount(
                id = "acc-other123456",
                userId = "other-user",
                accountNumber = "01654321",
                sortCode = "12-34-56",
                name = "Other User Account",
                accountType = "personal",
                balance = 2000.0,
                currency = "GBP",
                createdTimestamp = Instant.now(),
                updatedTimestamp = Instant.now()
            )
            accounts[otherUserAccount.accountNumber] = otherUserAccount
        }

        override fun createBankAccount(bankAccount: BankAccount): BankAccount {
            val newAccount = bankAccount.copy(
                id = if (bankAccount.id.isBlank()) "acc-${UUID.randomUUID()}" else bankAccount.id,
                createdTimestamp = Instant.now(),
                updatedTimestamp = Instant.now()
            )
            accounts[newAccount.accountNumber] = newAccount
            return newAccount
        }

        override fun getBankAccountByAccountNumber(accountNumber: String): BankAccount? = accounts[accountNumber]

        override fun updateBankAccount(accountNumber: String, bankAccount: BankAccount): BankAccount? {
            if (!accounts.containsKey(accountNumber)) return null
            val updatedAccount = bankAccount.copy(
                accountNumber = accountNumber,
                updatedTimestamp = Instant.now()
            )
            accounts[accountNumber] = updatedAccount
            return updatedAccount
        }

        override fun deleteBankAccount(accountNumber: String): Boolean {
            return accounts.remove(accountNumber) != null
        }

        override fun getBankAccountsByUserId(userId: String): List<BankAccount> =
            accounts.values.filter { it.userId == userId }

        override fun getAllBankAccounts(): List<BankAccount> = accounts.values.toList()
    }

    // Simple test implementation of ITransactionRepository
    class TestTransactionRepository : ITransactionRepository {
        private val transactions = mutableMapOf<String, Transaction>()

        init {
            // Add a test transaction for account "01123456"
            val testTransaction = Transaction(
                id = "txn-test123456",
                accountNumber = "01123456",
                userId = "test-user",
                amount = 100.0,
                currency = "GBP",
                type = TransactionType.DEPOSIT,
                reference = "Test deposit",
                createdTimestamp = Instant.now()
            )
            transactions[testTransaction.id] = testTransaction

            // Add a test transaction for account "01654321"
            val otherTransaction = Transaction(
                id = "txn-other123456",
                accountNumber = "01654321",
                userId = "other-user",
                amount = -50.0,
                currency = "GBP",
                type = TransactionType.WITHDRAWAL,
                reference = "Test withdrawal",
                createdTimestamp = Instant.now()
            )
            transactions[otherTransaction.id] = otherTransaction
        }

        override fun createTransaction(transaction: Transaction): Transaction {
            val newTransaction = transaction.copy(
                id = if (transaction.id.isBlank()) "txn-${UUID.randomUUID()}" else transaction.id,
                createdTimestamp = Instant.now()
            )
            transactions[newTransaction.id] = newTransaction
            return newTransaction
        }

        override fun getTransactionById(transactionId: String): Transaction? = transactions[transactionId]

        override fun getTransactionByIdAndAccountNumber(accountNumber: String, transactionId: String): Transaction? {
            val transaction = transactions[transactionId]
            return if (transaction != null && transaction.accountNumber == accountNumber) transaction else null
        }

        override fun getTransactionsByAccountNumber(accountNumber: String): List<Transaction> =
            transactions.values.filter { it.accountNumber == accountNumber }
    }

    // Simple test implementation of TransactionRequestValidationService
    class TestTransactionRequestValidationService : TransactionRequestValidationService {
        override fun validateCreateTransactionRequest(request: CreateTransactionRequest): List<ValidationDetail> {
            val errors = mutableListOf<ValidationDetail>()

            if (request.amount <= 0) {
                errors.add(ValidationDetail.invalidValue("amount", "Amount must be greater than zero"))
            }

            return errors
        }

        override fun validateSufficientFunds(accountBalance: Double, withdrawalAmount: Double): ValidationDetail? {
            return if (accountBalance < withdrawalAmount) {
                ValidationDetail.insufficientFunds("amount", "Insufficient funds for this withdrawal")
            } else {
                null
            }
        }
    }

    // Helper function to run test application
    private fun runTransactionTestApplication(
        block: suspend ApplicationTestBuilder.(client: io.ktor.client.HttpClient) -> Unit
    ) = testApplication {
        application {
            configureTestingModule()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }

        block(client)
    }

    @Test
    @DisplayName("POST /accounts/{accountNumber}/transactions with valid deposit should return Created")
    fun testCreateTransaction_withValidDeposit_returnsCreated() = runTransactionTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val response = client.post("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions") {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "amount": 50.0,
                    "currency": "GBP",
                    "type": "deposit",
                    "reference": "Test deposit"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    @DisplayName("POST /accounts/{accountNumber}/transactions with valid withdrawal should return Created")
    fun testCreateTransaction_withValidWithdrawal_returnsCreated() = runTransactionTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val response = client.post("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions") {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "amount": 50.0,
                    "currency": "GBP",
                    "type": "withdrawal",
                    "reference": "Test withdrawal"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    @DisplayName("POST /accounts/{accountNumber}/transactions with insufficient funds should return Unprocessable Entity")
    fun testCreateTransaction_withInsufficientFunds_returnsUnprocessableEntity() =
        runTransactionTestApplication { client ->
            val accountNumber = "01123456" // Account owned by test-user with balance 1000.0
            val response = client.post("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions") {
                bearerAuth(testToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                {
                    "amount": 2000.0,
                    "currency": "GBP",
                    "type": "withdrawal",
                    "reference": "Test withdrawal"
                }
                """.trimIndent()
                )
            }
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        }

    @Test
    @DisplayName("POST /accounts/{accountNumber}/transactions with invalid amount should return Bad Request")
    fun testCreateTransaction_withInvalidAmount_returnsBadRequest() = runTransactionTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val response = client.post("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions") {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "amount": 0.0,
                    "currency": "GBP",
                    "type": "deposit",
                    "reference": "Test deposit"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    @DisplayName("POST /accounts/{accountNumber}/transactions with another user's account should return Forbidden")
    fun testCreateTransaction_withAnotherUsersAccount_returnsForbidden() = runTransactionTestApplication { client ->
        val accountNumber = "01654321" // Account owned by other-user
        val response = client.post("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions") {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "amount": 50.0,
                    "currency": "GBP",
                    "type": "deposit",
                    "reference": "Test deposit"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    @DisplayName("GET /accounts/{accountNumber}/transactions with valid token and own account should return OK")
    fun testListTransactions_withValidTokenAndOwnAccount_returnsOk() = runTransactionTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val response = client.get("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("transactions"))
    }

    @Test
    @DisplayName("GET /accounts/{accountNumber}/transactions with another user's account should return Forbidden")
    fun testListTransactions_withAnotherUsersAccount_returnsForbidden() = runTransactionTestApplication { client ->
        val accountNumber = "01654321" // Account owned by other-user
        val response = client.get("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    @DisplayName("GET /accounts/{accountNumber}/transactions/{transactionId} with valid token and own account should return OK")
    fun testGetTransaction_withValidTokenAndOwnAccount_returnsOk() = runTransactionTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val transactionId = "txn-test123456" // Transaction for test-user's account
        val response = client.get("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions/$transactionId") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    @DisplayName("GET /accounts/{accountNumber}/transactions/{transactionId} with another user's account should return Forbidden")
    fun testGetTransaction_withAnotherUsersAccount_returnsForbidden() = runTransactionTestApplication { client ->
        val accountNumber = "01654321" // Account owned by other-user
        val transactionId = "txn-other123456" // Transaction for other-user's account
        val response = client.get("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions/$transactionId") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    @DisplayName("GET /accounts/{accountNumber}/transactions/{transactionId} with non-existent transaction ID should return Not Found")
    fun testGetTransaction_withNonExistentTransactionId_returnsNotFound() = runTransactionTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val nonExistentTransactionId = "txn-nonexistent"
        val response = client.get("$TRANSACTIONS_ENDPOINT/$accountNumber/transactions/$nonExistentTransactionId") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}