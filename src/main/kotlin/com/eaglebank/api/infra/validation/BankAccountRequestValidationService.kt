package com.eaglebank.api.infra.validation

import com.eaglebank.api.presentation.dto.CreateBankAccountRequest
import com.eaglebank.api.presentation.dto.UpdateBankAccountRequest
import com.eaglebank.api.presentation.dto.ValidationDetail

/**
 * Extension function to validate if a string is required.
 */
fun String?.validateRequired(fieldName: String): ValidationDetail? =
    if (this.isNullOrBlank()) ValidationDetail.required(fieldName)
    else null

/**
 * Service for validating bank account requests.
 */
interface BankAccountRequestValidationService {
    /**
     * Validates a CreateBankAccountRequest and returns a list of validation details.
     * An empty list indicates no validation errors were found.
     *
     * @param request The CreateBankAccountRequest to validate
     * @return List of ValidationDetail objects describing any validation errors
     */
    fun validateCreateBankAccountRequest(request: CreateBankAccountRequest): List<ValidationDetail>

    /**
     * Validates a UpdateBankAccountRequest and returns a list of validation details.
     * An empty list indicates no validation errors were found.
     *
     * @param request The UpdateBankAccountRequest to validate
     * @return List of ValidationDetail objects describing any validation errors
     */
    fun validateUpdateBankAccountRequest(request: UpdateBankAccountRequest): List<ValidationDetail>

    /**
     * Validates a sort code string.
     *
     * @param sortCode The sort code to validate
     * @return ValidationDetail if the sort code is invalid, null otherwise
     */
    fun validateSortCode(sortCode: String): ValidationDetail?

    /**
     * Validates an account number string.
     *
     * @param accountNumber The account number to validate
     * @return ValidationDetail if the account number is invalid, null otherwise
     */
    fun validateAccountNumber(accountNumber: String): ValidationDetail?
}

/**
 * Implementation of BankAccountRequestValidationService.
 */
class SimpleBankAccountRequestValidationService : BankAccountRequestValidationService {
    companion object {
        private object ValidationPatterns {
            /**
             * Regular expression for validating UK sort codes.
             * Format: XX-XX-XX where X is a digit.
             */
            val SORT_CODE = Regex("^\\d{2}-\\d{2}-\\d{2}$")

            /**
             * Regular expression for validating bank account numbers.
             * Format: 01 followed by 6 digits.
             */
            val ACCOUNT_NUMBER = Regex("^01\\d{6}$")
        }
    }

    override fun validateCreateBankAccountRequest(request: CreateBankAccountRequest): List<ValidationDetail> {
        return buildList {
            request.name.validateRequired("name")?.let { add(it) }
        }
    }

    override fun validateUpdateBankAccountRequest(request: UpdateBankAccountRequest): List<ValidationDetail> {
        return buildList {
            // No required fields in update request, as all fields are optional
        }
    }

    override fun validateSortCode(sortCode: String): ValidationDetail? {
        return if (!sortCode.matches(ValidationPatterns.SORT_CODE)) {
            ValidationDetail.invalidFormat("sortCode", "Must be in format: XX-XX-XX where X is a digit")
        } else {
            null
        }
    }

    override fun validateAccountNumber(accountNumber: String): ValidationDetail? {
        return if (!accountNumber.matches(ValidationPatterns.ACCOUNT_NUMBER)) {
            ValidationDetail.invalidFormat("accountNumber", "Must be in format: 01 followed by 6 digits")
        } else {
            null
        }
    }
}
