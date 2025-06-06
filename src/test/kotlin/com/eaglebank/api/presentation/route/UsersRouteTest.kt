package com.eaglebank.api.route

import com.eaglebank.api.application.configureOpenAPI
import com.eaglebank.api.application.configureRouting
import com.eaglebank.api.application.configureSecurity
import com.eaglebank.api.infra.di.serviceModule
import com.eaglebank.api.infra.validation.UserRequestValidationService
import com.eaglebank.api.presentation.dto.*
import com.eaglebank.api.util.withConfig
import com.typesafe.config.ConfigFactory
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.test.KoinTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

@DisplayName("Test suite for the UsersRoute")
class UsersRouteTest : KoinTest {
    private val config = withConfig(
        ConfigFactory.load("application-test.conf")
    ) {
        it
    }

    private val USERS_ENDPOINT = "/v1/users"

    // Helper function to define the common test application setup
    private fun runUserTestApplication(
        mockValidationService: (UserRequestValidationService) -> Unit,
        block: suspend ApplicationTestBuilder.(client: io.ktor.client.HttpClient) -> Unit
    ) = testApplication {
        environment {
            this.config = HoconApplicationConfig(this@UsersRouteTest.config)
        }

        application {
            // Manually install Koin first with the serviceModule
            install(Koin) {
                slf4jLogger()
                modules(serviceModule) // Load your actual serviceModule
            }

            // Load test-specific Koin modules to override, *after* main modules are loaded
            loadKoinModules(module() {
                single<UserRequestValidationService> {
                    mockk<UserRequestValidationService>().also { mockValidationService(it) }
                }
            })

            // Explicitly call the configuration functions
            configureSecurity(environment.config)
            configureRouting()
            configureOpenAPI()
        }

        val client = createClient {
            // Use the aliased ClientContentNegotiation for the client
            install(ClientContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }

        block(client)
    }

    @Test
    @DisplayName("Creating a user with a valid request body should succeed")
    fun testCreateUserRequestRoot_withValidRequestBody_succeeds() = runUserTestApplication(
        mockValidationService = { mock ->
            every { mock.validateCreateUserRequest(any()) } returns emptyList()
        }
    ) { client ->
        val createUserRequest = CreateUserRequestBuilder().build()

        val response = client.post(USERS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(createUserRequest)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val userResponse = response.body<UserResponse>()

        val expectedName = createUserRequest.name
        val expectedEmail = createUserRequest.email
        val expectedPhoneNumber = createUserRequest.phoneNumber

        assertNotNull(userResponse.id)
        assertEquals(expectedName, userResponse.name)
        assertEquals(expectedEmail, userResponse.email)
        assertEquals(expectedPhoneNumber, userResponse.phoneNumber)
    }

    @Test
    @DisplayName("Creating a user with a missing required field should return BadRequest")
    fun testCreateUserRequest_missingRequiredField_returnsBadRequest() = runUserTestApplication(
        mockValidationService = { mock ->
            every { mock.validateCreateUserRequest(any()) } returns listOf(
                ValidationDetail.required("name")
            )
        }
    ) { client ->
        val createUserRequest = CreateUserRequestBuilder().withName("").build()

        val response = client.post(USERS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(createUserRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorResponse = response.body<BadRequestErrorResponse>()
        assertNotNull(errorResponse.details)
        assertTrue(errorResponse.details.any { it.field == "name" && it.type == ValidationType.REQUIRED_FIELD.name })
    }

    @Test
    @DisplayName("Creating a user with an invalid format field should return BadRequest")
    fun testCreateUserRequest_invalidFormatField_returnsBadRequest() = runUserTestApplication(
        mockValidationService = { mock ->
            every { mock.validateCreateUserRequest(any()) } returns listOf(
                ValidationDetail.invalidFormat("email")
            )
        }
    ) { client ->
        val createUserRequest = CreateUserRequestBuilder().withEmail("invalid-email-format").build()

        val response = client.post(USERS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(createUserRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorResponse = response.body<BadRequestErrorResponse>()
        assertNotNull(errorResponse.details)
        assertTrue(errorResponse.details.any { it.field == "email" && it.type == ValidationType.INVALID_FORMAT.name })
    }
}

/**
 * Builder class for constructing instances of [CreateUserRequest].
 *
 * This class provides a fluent API for building a [CreateUserRequest] by allowing
 * incremental configuration of its properties.
 *
 * It ensures immutability of the resulting object once it is built.
 *
 * Each configuration method applies the specified value to the instance being built and
 * returns the builder object, facilitating method chaining.
 *
 * Default values:
 * - `name`: "John Doe"
 * - `address`: Default [Address] with placeholder values.
 * - `phoneNumber`: "+44-1234-5678"
 * - `email`: "john.doe@example.com"
 *
 * Functions:
 * - [withName]: Sets the name of the user.
 * - [withAddress]: Sets the address of the user.
 * - [withPhoneNumber]: Sets the phone number of the user.
 * - [withEmail]: Sets the email address of the user.
 * - [build]: Creates and returns a [CreateUserRequest] with the configured values.
 */
class CreateUserRequestBuilder {
    private var name: String = "John Doe"
    private var address: Address = Address(
        line1 = "123 Main St",
        line2 = "Apt 4",
        town = "Anytown",
        county = "Anyshire",
        postcode = "SW1A 0AA"
    )
    private var phoneNumber: String = "+44-1234-5678"
    private var email: String = "john.doe@example.com"

    fun withName(name: String) = apply { this.name = name }
    fun withAddress(address: Address) = apply { this.address = address }
    fun withPhoneNumber(phoneNumber: String) = apply { this.phoneNumber = phoneNumber }
    fun withEmail(email: String) = apply { this.email = email }

    fun build() = CreateUserRequest(
        name = name,
        address = address,
        phoneNumber = phoneNumber,
        email = email
    )
}