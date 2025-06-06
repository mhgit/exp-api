package com.eaglebank.api.infra.validation

import com.eaglebank.api.presentation.dto.Address
import com.eaglebank.api.presentation.dto.CreateUserRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName // Import DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Test suite for the Validation Service Tests") // Display name for the test class
class ValidationServiceTest {
    private lateinit var validationService: SimpleUserRequestValidationService

    private val validAddress = Address(
        line1 = "123 Test Street",
        town = "Test Town",
        county = "Test County",
        postcode = "SW1A 1AA"
    )

    private val validRequest = CreateUserRequest(
        name = "John Doe",
        email = "john.doe@example.com",
        phoneNumber = "+441234567899999",
        address = validAddress
    )

    @BeforeEach
    fun setup() {
        validationService = SimpleUserRequestValidationService()
    }

    @Nested
    @DisplayName("Name Validation") // Display name for the nested class
    inner class NameValidation {
        @Test
        @DisplayName("should accept valid name") // Display name for the test method
        fun `should accept valid name`() {
            val result = validationService.validateCreateUserRequest(validRequest)
            assertTrue(result.none { it.field == "name" })
        }

        @Test
        @DisplayName("should reject empty name")
        fun `should reject empty name`() {
            val request = validRequest.copy(name = "")
            val result = validationService.validateCreateUserRequest(request)
            assertEquals("name", result.first { it.field == "name" }.field)
            assertEquals("REQUIRED_FIELD", result.first { it.field == "name" }.type)
        }

        @Test
        @DisplayName("should reject blank name")
        fun `should reject blank name`() {
            val request = validRequest.copy(name = "   ")
            val result = validationService.validateCreateUserRequest(request)
            assertEquals("name", result.first { it.field == "name" }.field)
            assertEquals("REQUIRED_FIELD", result.first { it.field == "name" }.type)
        }
    }

    @Nested
    @DisplayName("Email Validation")
    inner class EmailValidation {
        @Test
        @DisplayName("should accept valid email")
        fun `should accept valid email`() {
            val invalidEmails = listOf(
                "invalid-email",
                "no@domain",
                "@nodomain.com",
                "spaces in@email.com"
            )

            invalidEmails.forEach { email ->
                val request = validRequest.copy(email = email)
                val result = validationService.validateCreateUserRequest(request)

                // First, verify that we have exactly one email validation error
                val emailErrors = result.filter { it.field == "email" }
                assertEquals(1, emailErrors.size, "Expected one email validation error for '$email'")

                // Then verify its properties
                val emailError = emailErrors[0]
                assertEquals("email", emailError.field)
                assertEquals("INVALID_FORMAT", emailError.type)
            }

        }
    }

    @Nested
    @DisplayName("Phone Number Validation")
    inner class PhoneNumberValidation {
        @Test
        @DisplayName("should accept valid phone number")
        fun `should accept valid phone number`() {
            val result = validationService.validateCreateUserRequest(validRequest)
            assertTrue(result.none { it.field == "phoneNumber" })
        }

        @Test
        @DisplayName("should reject invalid phone number formats")
        fun `should reject invalid phone number formats`() {
            val invalidPhones = listOf(
                "+1234567891234569",
                "+04-123-5678",
                "44-1234-5678",
                "+44-12345-5678",
                "+442-1234-5678"
            )

            invalidPhones.forEach { phone ->
                val request = validRequest.copy(phoneNumber = phone)
                val result = validationService.validateCreateUserRequest(request)
                assertEquals("phoneNumber", result.first { it.field == "phoneNumber" }.field)
                assertEquals("INVALID_FORMAT", result.first { it.field == "phoneNumber" }.type)
            }
        }
    }

    @Nested
    @DisplayName("Address Validation")
    inner class AddressValidation {
        @Test
        @DisplayName("should accept valid address")
        fun `should accept valid address`() {
            val result = validationService.validateCreateUserRequest(validRequest)
            assertTrue(result.none { it.field.startsWith("address") })
        }

        @Test
        @DisplayName("should reject empty line1")
        fun `should reject empty line1`() {
            val invalidAddress = validAddress.copy(line1 = "")
            val request = validRequest.copy(address = invalidAddress)
            val result = validationService.validateCreateUserRequest(request)
            assertEquals("address.line1", result.first { it.field == "address.line1" }.field)
            assertEquals("REQUIRED_FIELD", result.first { it.field == "address.line1" }.type)
        }

        @Test
        @DisplayName("should accept optional line2 and line3")
        fun `should accept optional line2 and line3`() {
            val address = validAddress.copy(line2 = null, line3 = null)
            val request = validRequest.copy(address = address)
            val result = validationService.validateCreateUserRequest(request)
            assertTrue(result.none { it.field.startsWith("address.line2") })
            assertTrue(result.none { it.field.startsWith("address.line3") })
        }

        @Test
        @DisplayName("should reject empty town")
        fun `should reject empty town`() {
            val invalidAddress = validAddress.copy(town = "")
            val request = validRequest.copy(address = invalidAddress)
            val result = validationService.validateCreateUserRequest(request)
            assertEquals("address.town", result.first { it.field == "address.town" }.field)
            assertEquals("REQUIRED_FIELD", result.first { it.field == "address.town" }.type)
        }

        @Test
        @DisplayName("should reject empty county")
        fun `should reject empty county`() {
            val invalidAddress = validAddress.copy(county = "")
            val request = validRequest.copy(address = invalidAddress)
            val result = validationService.validateCreateUserRequest(request)
            assertEquals("address.county", result.first { it.field == "address.county" }.field)
            assertEquals("REQUIRED_FIELD", result.first { it.field == "address.county" }.type)
        }

        @Test
        @DisplayName("should validate postcode format")
        fun `should validate postcode format`() {
            val validPostcodes = listOf(
                "SW1A 1AA",
                "M1 1AA",
                "B33 8TH",
                "CR2 6XH",
                "DN55 1PT"
            )

            val invalidPostcodes = listOf(
                "invalid",
                "12345",
                "ABC DEF",
                "A1A 99AA",
                "AA1A 9AAA"
            )

            validPostcodes.forEach { postcode ->
                val address = validAddress.copy(postcode = postcode)
                val request = validRequest.copy(address = address)
                val result = validationService.validateCreateUserRequest(request)
                assertTrue(result.none { it.field == "address.postcode" })
            }

            invalidPostcodes.forEach { postcode ->
                val address = validAddress.copy(postcode = postcode)
                val request = validRequest.copy(address = address)
                val result = validationService.validateCreateUserRequest(request)
                assertEquals("address.postcode", result.first { it.field == "address.postcode" }.field)
                assertEquals("INVALID_FORMAT", result.first { it.field == "address.postcode" }.type)
            }
        }
    }

    @Test
    @DisplayName("should return multiple validation errors when multiple fields are invalid")
    fun `should return multiple validation errors when multiple fields are invalid`() {
        val invalidAddress = Address(
            line1 = "",
            town = "",
            county = "Test County",
            postcode = "invalid"
        )

        val request = CreateUserRequest(
            name = "",
            email = "invalid-email",
            phoneNumber = "invalid-phone",
            address = invalidAddress
        )

        val result = validationService.validateCreateUserRequest(request)
        assertEquals(6, result.size)
        assertTrue(result.any { it.field == "name" && it.type == "REQUIRED_FIELD" })
        assertTrue(result.any { it.field == "email" && it.type == "INVALID_FORMAT" })
        assertTrue(result.any { it.field == "phoneNumber" && it.type == "INVALID_FORMAT" })
        assertTrue(result.any { it.field == "address.line1" && it.type == "REQUIRED_FIELD" })
        assertTrue(result.any { it.field == "address.town" && it.type == "REQUIRED_FIELD" })
        assertTrue(result.any { it.field == "address.postcode" && it.type == "INVALID_FORMAT" })
    }
}