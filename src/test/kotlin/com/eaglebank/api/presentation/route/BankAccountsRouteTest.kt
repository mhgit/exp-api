package com.eaglebank.api.presentation.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.eaglebank.api.domain.model.BankAccount
import com.eaglebank.api.domain.repository.IBankAccountRepository
import com.eaglebank.api.infra.validation.BankAccountRequestValidationService
import com.eaglebank.api.presentation.dto.CreateBankAccountRequest
import com.eaglebank.api.presentation.dto.UpdateBankAccountRequest
import com.eaglebank.api.presentation.dto.ValidationDetail
import com.eaglebank.api.presentation.route.bankAccountsRoute
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

@DisplayName("Test suite for the BankAccountsRoute")
class BankAccountsRouteTest : KoinTest {
    private val ACCOUNTS_ENDPOINT = "/v1/accounts"

    // Mock JWT token for testing with required claims
    private val testToken = JWT.create()
        .withIssuer("http://localhost:8082/auth/realms/eagle-bank")
        .withSubject("test-user")
        .withClaim("email", "test@example.com")
        .sign(Algorithm.none())

    // Test dependencies
    private val testBankAccountRepository = TestBankAccountRepository()
    private val testValidationService = TestBankAccountRequestValidationService()

    @BeforeTest
    fun setup() {
        stopKoin() // Stop any existing Koin instance
        startKoin {
            modules(module {
                single<IBankAccountRepository> { testBankAccountRepository }
                single<BankAccountRequestValidationService> { testValidationService }
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
            bankAccountsRoute()
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
                id = if (bankAccount.id.isBlank()) "acc-${java.util.UUID.randomUUID()}" else bankAccount.id,
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

    // Simple test implementation of BankAccountRequestValidationService
    class TestBankAccountRequestValidationService : BankAccountRequestValidationService {
        override fun validateCreateBankAccountRequest(request: CreateBankAccountRequest): List<ValidationDetail> {
            // For testing, we'll just do minimal validation
            val errors = mutableListOf<ValidationDetail>()

            if (request.name.isBlank()) {
                errors.add(ValidationDetail.required("name"))
            }

            return errors
        }

        override fun validateUpdateBankAccountRequest(request: UpdateBankAccountRequest): List<ValidationDetail> {
            // No required fields in update request
            return emptyList()
        }

        override fun validateSortCode(sortCode: String): ValidationDetail? {
            return if (!sortCode.matches(Regex("^\\d{2}-\\d{2}-\\d{2}$"))) {
                ValidationDetail.invalidFormat("sortCode", "Must be in format: XX-XX-XX where X is a digit")
            } else {
                null
            }
        }

        override fun validateAccountNumber(accountNumber: String): ValidationDetail? {
            return if (!accountNumber.matches(Regex("^01\\d{6}$"))) {
                ValidationDetail.invalidFormat("accountNumber", "Must be in format: 01 followed by 6 digits")
            } else {
                null
            }
        }
    }

    // Then in the test application setup:
    private fun runBankAccountTestApplication(
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
    @DisplayName("PATCH /accounts/{accountNumber} with valid token and own account should return OK")
    fun testUpdateAccount_withValidTokenAndOwnAccount_returnsOk() = runBankAccountTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val response = client.patch("$ACCOUNTS_ENDPOINT/$accountNumber") {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "name": "Updated Account Name"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    @DisplayName("PATCH /accounts/{accountNumber} with valid token but another user's account should return Forbidden")
    fun testUpdateAccount_withValidTokenButAnotherUsersAccount_returnsForbidden() =
        runBankAccountTestApplication { client ->
            val accountNumber = "01654321" // Account owned by other-user
            val response = client.patch("$ACCOUNTS_ENDPOINT/$accountNumber") {
                bearerAuth(testToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                {
                    "name": "Updated Account Name"
                }
                """.trimIndent()
                )
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    @DisplayName("PATCH /accounts/{accountNumber} with non-existent account number should return Not Found")
    fun testUpdateAccount_withNonExistentAccountNumber_returnsNotFound() = runBankAccountTestApplication { client ->
        val nonExistentAccountNumber = "01999999"
        val response = client.patch("$ACCOUNTS_ENDPOINT/$nonExistentAccountNumber") {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "name": "Updated Account Name"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    @DisplayName("DELETE /accounts/{accountNumber} with valid token and own account should return No Content")
    fun testDeleteAccount_withValidTokenAndOwnAccount_returnsNoContent() = runBankAccountTestApplication { client ->
        val accountNumber = "01123456" // Account owned by test-user
        val response = client.delete("$ACCOUNTS_ENDPOINT/$accountNumber") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    @DisplayName("DELETE /accounts/{accountNumber} with valid token but another user's account should return Forbidden")
    fun testDeleteAccount_withValidTokenButAnotherUsersAccount_returnsForbidden() =
        runBankAccountTestApplication { client ->
            val accountNumber = "01654321" // Account owned by other-user
            val response = client.delete("$ACCOUNTS_ENDPOINT/$accountNumber") {
                bearerAuth(testToken)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    @DisplayName("DELETE /accounts/{accountNumber} with non-existent account number should return Not Found")
    fun testDeleteAccount_withNonExistentAccountNumber_returnsNotFound() = runBankAccountTestApplication { client ->
        val nonExistentAccountNumber = "01999999"
        val response = client.delete("$ACCOUNTS_ENDPOINT/$nonExistentAccountNumber") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
