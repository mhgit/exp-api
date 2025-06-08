package com.eaglebank.api.infra.validation

import com.eaglebank.api.presentation.dto.CreateTransactionRequest
import com.eaglebank.api.presentation.dto.ValidationDetail

/**
 * Service for validating transaction requests.
 */
interface TransactionRequestValidationService {
    /**
     * Validates a CreateTransactionRequest and returns a list of validation details.
     * An empty list indicates no validation errors were found.
     *
     * @param request The CreateTransactionRequest to validate
     * @return List of ValidationDetail objects describing any validation errors
     */
    fun validateCreateTransactionRequest(request: CreateTransactionRequest): List<ValidationDetail>

    /**
     * Validates if an account has sufficient funds for a withdrawal.
     *
     * @param accountBalance The current account balance
     * @param withdrawalAmount The amount to withdraw
     * @return ValidationDetail if the account has insufficient funds, null otherwise
     */
    fun validateSufficientFunds(accountBalance: Double, withdrawalAmount: Double): ValidationDetail?
}

/**
 * Implementation of TransactionRequestValidationService.
 */
class SimpleTransactionRequestValidationService : TransactionRequestValidationService {
    override fun validateCreateTransactionRequest(request: CreateTransactionRequest): List<ValidationDetail> {
        return buildList {
            if (request.amount <= 0) {
                add(ValidationDetail.invalidValue("amount", "Amount must be greater than zero"))
            }
        }
    }

    override fun validateSufficientFunds(accountBalance: Double, withdrawalAmount: Double): ValidationDetail? {
        return if (accountBalance < withdrawalAmount) {
            ValidationDetail.insufficientFunds("amount", "Insufficient funds for this withdrawal")
        } else {
            null
        }
    }
}