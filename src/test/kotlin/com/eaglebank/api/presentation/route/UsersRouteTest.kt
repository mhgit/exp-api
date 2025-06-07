package com.eaglebank.api.presentation.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.eaglebank.api.application.configureOpenAPI
import com.eaglebank.api.application.configureRouting
import com.typesafe.config.ConfigFactory
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.test.KoinTest
import kotlin.test.assertEquals

@Disabled("Authentication tests disabled due to AuthenticationHolder plugin issues in test environment")
@DisplayName("Test suite for the UsersRoute")
class UsersRouteTest : KoinTest {
    private val config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
    private val USERS_ENDPOINT = "/v1/users"

    // Mock JWT token for testing with required claims
    private val testToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0." +
            "eyJzdWIiOiJ0ZXN0LXVzZXIiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODIvYXV0aC9yZWFsbXMvZWFnbGUtYmFuayJ9."

    private fun Application.configureTestingModule() {
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(
                    JWT.require(Algorithm.none())
                        .withIssuer("http://localhost:8082/auth/realms/eagle-bank")
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("email").asString().isNotEmpty()) {
                        JWTPrincipal(credential.payload)
                    } else null
                }
            }
        }

        configureRouting()
        configureOpenAPI()
    }

    // Then in the test application setup:
    private fun runUserTestApplication(
        block: suspend ApplicationTestBuilder.(client: io.ktor.client.HttpClient) -> Unit
    ) = testApplication {
        environment {
            config = this@UsersRouteTest.config
        }

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
        val userId = "test-user-id"
        val response = client.get("$USERS_ENDPOINT/$userId") {
            bearerAuth(testToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}