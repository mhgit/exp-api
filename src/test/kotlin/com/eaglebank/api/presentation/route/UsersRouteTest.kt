package com.eaglebank.api.route

import com.eaglebank.api.application.module
import com.eaglebank.api.infra.validation.SimpleUserRequestValidationService
import com.eaglebank.api.infra.validation.UserRequestValidationService
import com.eaglebank.api.presentation.dto.Address
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UserResponse
import com.eaglebank.api.presentation.dto.ValidationDetail
import com.eaglebank.api.util.withConfig
import com.typesafe.config.ConfigFactory
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull



class UsersRouteTest : KoinTest {

    private val config = withConfig(
        ConfigFactory.load("application-test.conf")
    ) {
        // You can override or add test-specific configurations here if needed
        it

    }

    @Test
    @DisplayName("Test: Creating a user with a valid request body should succeed")
    fun testCreateUserRequestRoot_withValidRequestBody_succeeds() = testApplication {
        application {
            module(config)
        }
        val createUserRequest = CreateUserRequest(
            name = "John Doe",
            address = Address(
                line1 = "123 Main St",
                line2 = "Apt 4",
                town = "Anytown",
                county = "Anyshire",
                postcode = "SW1A 0AA"
            ),
            phoneNumber = "+44-1234-5678",
            email = "john.doe@example.com"
        )
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
        val response = client.post("/v1/users") {
            contentType(ContentType.Application.Json)
            setBody(createUserRequest)
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val userResponse = response.body<UserResponse>()
        assertNotNull(userResponse.id)
        assertEquals(createUserRequest.name, userResponse.name)
        assertEquals(createUserRequest.email, userResponse.email)
        assertEquals(createUserRequest.phoneNumber, userResponse.phoneNumber)

    }




}