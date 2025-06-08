package com.eaglebank.api.presentation.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.eaglebank.api.domain.model.Address
import com.eaglebank.api.domain.model.User
import com.eaglebank.api.domain.repository.IUserRepository
import com.eaglebank.api.infra.validation.UserRequestValidationService
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.ValidationDetail
import com.eaglebank.api.presentation.route.usersRoute
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

@DisplayName("Test suite for the UsersRoute")
class UsersRouteTest : KoinTest {
    private val USERS_ENDPOINT = "/v1/users"

    // Mock JWT token for testing with required claims
    private val testToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0." +
            "eyJzdWIiOiJ0ZXN0LXVzZXIiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODIvYXV0aC9yZWFsbXMvZWFnbGUtYmFuayJ9."

    // Test dependencies
    private val testUserRepository = TestUserRepository()
    private val testValidationService = TestUserRequestValidationService()

    @BeforeTest
    fun setup() {
        stopKoin() // Stop any existing Koin instance
        startKoin {
            modules(module {
                single<IUserRepository> { testUserRepository }
                single<UserRequestValidationService> { testValidationService }
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
                    // Check that the email claim is not empty
                    if (credential.payload.getClaim("email").asString().isNotEmpty()) {
                        // For testing purposes, we'll accept all tokens with a valid email
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
            usersRoute()
        }
    }

    // Simple test implementation of IUserRepository
    class TestUserRepository : IUserRepository {
        private val users = mutableMapOf<String, User>()

        init {
            // Add a test user
            val testUser = User(
                id = "usr-test123456",
                name = "Test User",
                address = Address(
                    line1 = "123 Test St",
                    line2 = null,
                    line3 = null,
                    town = "Testville",
                    county = "Testshire",
                    postcode = "TE1 1ST"
                ),
                phoneNumber = "+441234567",
                email = "test@example.com",
                keycloakId = "test-user",
                createdTimestamp = Instant.now().toString(),
                updatedTimestamp = Instant.now().toString()
            )
            users[testUser.id] = testUser
        }

        override fun createUser(user: User): User {
            val newUser = user.copy(
                id = if (user.id.isBlank()) User.generateId() else user.id,
                createdTimestamp = Instant.now().toString(),
                updatedTimestamp = Instant.now().toString()
            )
            users[newUser.id] = newUser
            return newUser
        }

        override fun getUserById(id: String): User? = users[id]

        override fun updateUser(id: String, user: User): User? {
            if (!users.containsKey(id)) return null
            val updatedUser = user.copy(
                id = id,
                updatedTimestamp = Instant.now().toString()
            )
            users[id] = updatedUser
            return updatedUser
        }

        override fun deleteUser(id: String): Boolean {
            return users.remove(id) != null
        }

        override fun getAllUsers(): List<User> = users.values.toList()
    }

    // Simple test implementation of UserRequestValidationService
    class TestUserRequestValidationService : UserRequestValidationService {
        override fun validateCreateUserRequest(request: CreateUserRequest): List<ValidationDetail> {
            // For testing, we'll just do minimal validation
            val errors = mutableListOf<ValidationDetail>()

            if (request.name.isBlank()) {
                errors.add(ValidationDetail.required("name"))
            }

            if (request.email.isBlank()) {
                errors.add(ValidationDetail.required("email"))
            }

            return errors
        }
    }

    // Then in the test application setup:
    private fun runUserTestApplication(
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
    @DisplayName("GET /users with valid token should return OK")
    fun testGetUsers_withValidToken_returnsOk() = runUserTestApplication { client ->
        val response = client.get(USERS_ENDPOINT) {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    @DisplayName("GET /users without token should return Unauthorized")
    fun testGetUsers_withoutToken_returnsUnauthorized() = runUserTestApplication { client ->
        val response = client.get(USERS_ENDPOINT)
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    @DisplayName("GET /users with invalid token should return Unauthorized")
    fun testGetUsers_withInvalidToken_returnsUnauthorized() = runUserTestApplication { client ->
        val response = client.get(USERS_ENDPOINT) {
            bearerAuth("invalid.token.here")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    @DisplayName("GET /users/{id} with valid token should return OK")
    fun testGetUserById_withValidToken_returnsOk() = runUserTestApplication { client ->
        val userId = "usr-test123456"
        val response = client.get("$USERS_ENDPOINT/$userId") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    @DisplayName("GET /users/{id} with non-existent ID should return Not Found")
    fun testGetUserById_withNonExistentId_returnsNotFound() = runUserTestApplication { client ->
        val nonExistentId = "usr-nonexistent"
        val response = client.get("$USERS_ENDPOINT/$nonExistentId") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    @DisplayName("PATCH /users/{id} with non-existent ID should return Not Found")
    fun testUpdateUser_withNonExistentId_returnsNotFound() = runUserTestApplication { client ->
        val nonExistentId = "usr-nonexistent"
        val response = client.patch("$USERS_ENDPOINT/$nonExistentId") {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "name": "Updated Name",
                    "address": {
                        "line1": "Updated Line 1",
                        "town": "Updated Town",
                        "county": "Updated County",
                        "postcode": "UP1 1ST"
                    },
                    "phoneNumber": "+449876543",
                    "email": "updated@example.com"
                }
            """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    @DisplayName("DELETE /users/{id} with non-existent ID should return Not Found for admin users")
    fun testDeleteUser_withNonExistentId_returnsNotFound() = runUserTestApplication { client ->
        val nonExistentId = "usr-nonexistent"
        val adminToken = createAdminToken()
        val response = client.delete("$USERS_ENDPOINT/$nonExistentId") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    @DisplayName("DELETE /users/{id} with existing ID should return Forbidden for non-admin users")
    fun testDeleteUser_withExistingId_returnsForbiddenForNonAdmin() = runUserTestApplication { client ->
        val existingId = "usr-test123456"
        val response = client.delete("$USERS_ENDPOINT/$existingId") {
            bearerAuth(testToken) // Using non-admin token
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    @DisplayName("DELETE /users/{id} with non-existent ID should return Forbidden for non-admin users")
    fun testDeleteUser_withNonExistentId_returnsForbiddenForNonAdmin() = runUserTestApplication { client ->
        val nonExistentId = "usr-nonexistent"
        val response = client.delete("$USERS_ENDPOINT/$nonExistentId") {
            bearerAuth(testToken) // Using non-admin token
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    // Helper function to create an admin token for testing
    private fun createAdminToken(): String {
        return JWT.create()
            .withIssuer("http://localhost:8082/auth/realms/eagle-bank")
            .withSubject("admin-user")
            .withClaim("email", "admin@example.com")
            .withClaim("realm_access", mapOf("roles" to listOf("admin")))
            .sign(Algorithm.none())
    }
}
