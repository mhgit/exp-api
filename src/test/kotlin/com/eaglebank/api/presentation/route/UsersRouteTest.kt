package com.eaglebank.api.route

import com.eaglebank.api.application.module
import com.eaglebank.api.infra.validation.UserRequestValidationService
import com.eaglebank.api.presentation.dto.Address
import com.eaglebank.api.presentation.dto.BadRequestErrorResponse
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UserResponse
import com.eaglebank.api.presentation.dto.ValidationDetail
import com.eaglebank.api.presentation.dto.ValidationType
import com.eaglebank.api.util.withConfig
import com.typesafe.config.ConfigFactory
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("Test suite for the UsersRoute")
class UsersRouteTest : KoinTest {
    private val config = withConfig(
        ConfigFactory.load("application-test.conf")
    ) {
        // Override or add test-specific configurations here if needed
        it
    }

    private val USERS_ENDPOINT = "/v1/users"

    @Test
    @DisplayName("Creating a user with a valid request body should succeed")
    fun testCreateUserRequestRoot_withValidRequestBody_succeeds() = testApplication {
        application {
            module(config)
            configureKoinModuleWithMocks()
        }

        val createUserRequest = CreateUserRequestBuilder().build()
        val client = createHttpClient()

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
    fun testCreateUserRequest_missingRequiredField_returnsBadRequest() = testApplication {
        application {
            module(config)
            configureKoinModuleWithMocksForMissingField()
        }

        val createUserRequest = CreateUserRequestBuilder().withName("").build() // Simulate missing name
        val client = createHttpClient()

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
    fun testCreateUserRequest_invalidFormatField_returnsBadRequest() = testApplication {
        application {
            module(config)
            configureKoinModuleWithMocksForInvalidFormat()
        }

        val createUserRequest = CreateUserRequestBuilder().withEmail("invalid-email-format").build()
        val client = createHttpClient()

        val response = client.post(USERS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(createUserRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorResponse = response.body<BadRequestErrorResponse>()
        assertNotNull(errorResponse.details)
        assertTrue(errorResponse.details.any { it.field == "email" && it.type == ValidationType.INVALID_FORMAT.name })
    }

    private fun configureKoinModuleWithMocks() {
        org.koin.core.context.loadKoinModules(
            module {
                single<UserRequestValidationService>() {
                    mockk<UserRequestValidationService> {
                        every { validateCreateUserRequest(any()) } returns emptyList()
                    }
                }
            }
        )
    }

    private fun configureKoinModuleWithMocksForMissingField() {
        org.koin.core.context.loadKoinModules(
            module {
                single<UserRequestValidationService>() {
                    mockk<UserRequestValidationService> {
                        every { validateCreateUserRequest(any()) } returns listOf(
                            ValidationDetail.required("name")
                        )
                    }
                }
            }
        )
    }

    private fun configureKoinModuleWithMocksForInvalidFormat() {
        org.koin.core.context.loadKoinModules(
            module {
                single<UserRequestValidationService>() {
                    mockk<UserRequestValidationService> {
                        every { validateCreateUserRequest(any()) } returns listOf(
                            ValidationDetail.invalidFormat("email")
                        )
                    }
                }
            }
        )
    }

    private fun ApplicationTestBuilder.createHttpClient() = createClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
}

/**
 * Builder class for constructing instances of [CreateUserRequest].
 *
 * This class provides a fluent API for building a [CreateUserRequest] by allowing
 * incremental configuration of its properties. It ensures immutability of the resulting
 * object once it is built.
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