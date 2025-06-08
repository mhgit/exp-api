package com.eaglebank.api.infra.validation

import com.eaglebank.api.presentation.dto.AccountType
import com.eaglebank.api.presentation.dto.CreateBankAccountRequest
import com.eaglebank.api.presentation.dto.UpdateBankAccountRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Test suite for the Bank Account Request Validation Service")
class BankAccountRequestValidationServiceTest {
    private lateinit var validationService: SimpleBankAccountRequestValidationService

    private val validCreateRequest = CreateBankAccountRequest(
        name = "Test Account",
        accountType = AccountType.personal
    )

    private val validUpdateRequest = UpdateBankAccountRequest(
        name = "Updated Account",
        accountType = AccountType.personal
    )

    @BeforeEach
    fun setup() {
        validationService = SimpleBankAccountRequestValidationService()
    }

    @Nested
    @DisplayName("Create Bank Account Request Validation")
    inner class CreateBankAccountRequestValidation {
        @Test
        @DisplayName("should accept valid request")
        fun `should accept valid request`() {
            val result = validationService.validateCreateBankAccountRequest(validCreateRequest)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("should reject empty name")
        fun `should reject empty name`() {
            val request = validCreateRequest.copy(name = "")
            val result = validationService.validateCreateBankAccountRequest(request)
            assertEquals(1, result.size)
            assertEquals("name", result[0].field)
            assertEquals("REQUIRED_FIELD", result[0].type)
        }

        @Test
        @DisplayName("should reject blank name")
        fun `should reject blank name`() {
            val request = validCreateRequest.copy(name = "   ")
            val result = validationService.validateCreateBankAccountRequest(request)
            assertEquals(1, result.size)
            assertEquals("name", result[0].field)
            assertEquals("REQUIRED_FIELD", result[0].type)
        }
    }

    @Nested
    @DisplayName("Update Bank Account Request Validation")
    inner class UpdateBankAccountRequestValidation {
        @Test
        @DisplayName("should accept valid request")
        fun `should accept valid request`() {
            val result = validationService.validateUpdateBankAccountRequest(validUpdateRequest)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("should accept request with null fields")
        fun `should accept request with null fields`() {
            val request = UpdateBankAccountRequest()
            val result = validationService.validateUpdateBankAccountRequest(request)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Sort Code Validation")
    inner class SortCodeValidation {
        @Test
        @DisplayName("should accept valid sort codes")
        fun `should accept valid sort codes`() {
            val validSortCodes = listOf(
                "12-34-56",
                "00-00-00",
                "99-99-99"
            )

            validSortCodes.forEach { sortCode ->
                val result = validationService.validateSortCode(sortCode)
                assertEquals(null, result)
            }
        }

        @Test
        @DisplayName("should reject invalid sort codes")
        fun `should reject invalid sort codes`() {
            val invalidSortCodes = listOf(
                "1234-56",
                "12-3456",
                "12-34-5",
                "123-45-6",
                "12-34-567",
                "12-34-5a",
                "a2-34-56",
                "12-a4-56",
                "123456",
                "12 34 56",
                ""
            )

            invalidSortCodes.forEach { sortCode ->
                val result = validationService.validateSortCode(sortCode)
                assertEquals("sortCode", result?.field)
                assertEquals("INVALID_FORMAT", result?.type)
            }
        }
    }

    @Nested
    @DisplayName("Account Number Validation")
    inner class AccountNumberValidation {
        @Test
        @DisplayName("should accept valid account numbers")
        fun `should accept valid account numbers`() {
            val validAccountNumbers = listOf(
                "01123456",
                "01000000",
                "01999999"
            )

            validAccountNumbers.forEach { accountNumber ->
                val result = validationService.validateAccountNumber(accountNumber)
                assertEquals(null, result)
            }
        }

        @Test
        @DisplayName("should reject invalid account numbers")
        fun `should reject invalid account numbers`() {
            val invalidAccountNumbers = listOf(
                "0112345", // Too short
                "011234567", // Too long
                "00123456", // Doesn't start with 01
                "02123456", // Doesn't start with 01
                "0112345a", // Contains non-digit
                "a1123456", // Contains non-digit
                "", // Empty
                "123456", // Missing prefix
                "01 12345" // Contains space
            )

            invalidAccountNumbers.forEach { accountNumber ->
                val result = validationService.validateAccountNumber(accountNumber)
                assertEquals("accountNumber", result?.field)
                assertEquals("INVALID_FORMAT", result?.type)
            }
        }
    }
}