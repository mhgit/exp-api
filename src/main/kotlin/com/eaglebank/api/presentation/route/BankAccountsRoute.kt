package com.eaglebank.api.presentation.route

import com.eaglebank.api.domain.repository.IBankAccountRepository
import com.eaglebank.api.infra.security.RoleBasedAuthorization
import com.eaglebank.api.infra.validation.BankAccountRequestValidationService
import com.eaglebank.api.presentation.dto.CreateBankAccountRequest
import com.eaglebank.api.presentation.dto.ListBankAccountsResponse
import com.eaglebank.api.presentation.dto.UpdateBankAccountRequest
import com.eaglebank.api.presentation.mapper.BankAccountMapper
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * Route handler for bank account endpoints.
 */
fun Route.bankAccountsRoute() {
    val logger = LoggerFactory.getLogger("BankAccountsRoute")
    val bankAccountRepository: IBankAccountRepository by inject()
    val validationService: BankAccountRequestValidationService by inject()

    route("/v1/accounts") {
        // All endpoints are protected with JWT authentication
        authenticate("auth-jwt") {
            // POST endpoint to create a new bank account
            post {
                logger.info("Received create bank account request")
                try {
                    val request = call.receive<CreateBankAccountRequest>()
                    logger.debug("Processing create bank account request for name: ${request.name}")

                    // Validate the request
                    val validationErrors = validationService.validateCreateBankAccountRequest(request)
                    if (validationErrors.isNotEmpty()) {
                        logger.warn("Validation failed for create bank account request: $validationErrors")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "message" to "Validation failed",
                                "errors" to validationErrors
                            )
                        )
                        return@post
                    }

                    // Get the user ID from the JWT token
                    val userId = RoleBasedAuthorization.getUserId(call)
                    if (userId == null) {
                        logger.warn("User ID not found in token")
                        call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "User ID not found in token"))
                        return@post
                    }

                    // Create the bank account
                    val domainBankAccount = BankAccountMapper.createRequestToDomain(request, userId)
                    val createdBankAccount = bankAccountRepository.createBankAccount(domainBankAccount)
                    logger.info("Successfully created bank account with account number: ${createdBankAccount.accountNumber}")

                    call.respond(HttpStatusCode.Created, BankAccountMapper.toBankAccountResponse(createdBankAccount))
                } catch (e: Exception) {
                    logger.error("Failed to create bank account", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "Failed to create bank account: ${e.message}")
                    )
                }
            }

            // GET endpoint to list all bank accounts for the authenticated user
            get {
                logger.info("Received list bank accounts request")
                try {
                    // Get the user ID from the JWT token
                    val userId = RoleBasedAuthorization.getUserId(call)
                    if (userId == null) {
                        logger.warn("User ID not found in token")
                        call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "User ID not found in token"))
                        return@get
                    }

                    // Get all bank accounts for the user
                    val bankAccounts = bankAccountRepository.getBankAccountsByUserId(userId)
                        .map(BankAccountMapper::toBankAccountResponse)
                    logger.debug("Retrieved ${bankAccounts.size} bank accounts for user $userId")

                    call.respond(HttpStatusCode.OK, ListBankAccountsResponse(accounts = bankAccounts))
                } catch (e: Exception) {
                    logger.error("Failed to list bank accounts", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "Failed to list bank accounts: ${e.message}")
                    )
                }
            }

            // GET endpoint to fetch a specific bank account by account number
            get("{accountNumber}") {
                val accountNumber = call.parameters["accountNumber"]
                logger.info("Received get bank account request for account number: $accountNumber")

                if (accountNumber == null) {
                    logger.warn("Account number is null")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Account number is required"))
                    return@get
                }

                val validationError = validationService.validateAccountNumber(accountNumber)
                if (validationError != null) {
                    logger.warn("Invalid account number format: $accountNumber")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "message" to "Validation failed",
                            "errors" to listOf(validationError)
                        )
                    )
                    return@get
                }

                try {
                    // Get the bank account
                    val bankAccount = bankAccountRepository.getBankAccountByAccountNumber(accountNumber)
                    if (bankAccount == null) {
                        logger.warn("Bank account not found with account number: $accountNumber")
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Bank account not found"))
                        return@get
                    }

                    // Check if the user has permission to access this bank account
                    val userId = RoleBasedAuthorization.getUserId(call)
                    val principal = call.principal<JWTPrincipal>()
                    val hasAdminRole = RoleBasedAuthorization.hasRole(principal, "admin")
                    val isOwnAccount = userId == bankAccount.userId

                    if (isOwnAccount || hasAdminRole) {
                        logger.debug("Successfully retrieved bank account with account number: $accountNumber")
                        call.respond(HttpStatusCode.OK, BankAccountMapper.toBankAccountResponse(bankAccount))
                    } else {
                        logger.warn("Access denied: User $userId attempted to access bank account $accountNumber")
                        call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("message" to "Access denied: Insufficient permissions")
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get bank account", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "Failed to get bank account: ${e.message}")
                    )
                }
            }

            // PATCH endpoint to update a bank account
            patch("{accountNumber}") {
                val accountNumber = call.parameters["accountNumber"]
                logger.info("Received update bank account request for account number: $accountNumber")

                if (accountNumber == null) {
                    logger.warn("Account number is null")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Account number is required"))
                    return@patch
                }

                val validationError = validationService.validateAccountNumber(accountNumber)
                if (validationError != null) {
                    logger.warn("Invalid account number format: $accountNumber")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "message" to "Validation failed",
                            "errors" to listOf(validationError)
                        )
                    )
                    return@patch
                }

                try {
                    // Get the existing bank account
                    val existingAccount = bankAccountRepository.getBankAccountByAccountNumber(accountNumber)
                    if (existingAccount == null) {
                        logger.warn("Bank account not found with account number: $accountNumber")
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Bank account not found"))
                        return@patch
                    }

                    // Check if the user has permission to update this bank account
                    val userId = RoleBasedAuthorization.getUserId(call)
                    val isOwnAccount = userId == existingAccount.userId

                    if (!isOwnAccount) {
                        logger.warn("Access denied: User $userId attempted to update bank account $accountNumber")
                        call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("message" to "Access denied: Insufficient permissions")
                        )
                        return@patch
                    }

                    // Parse and validate the update request
                    val request = call.receive<UpdateBankAccountRequest>()
                    val validationErrors = validationService.validateUpdateBankAccountRequest(request)
                    if (validationErrors.isNotEmpty()) {
                        logger.warn("Validation failed for update bank account request: $validationErrors")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "message" to "Validation failed",
                                "errors" to validationErrors
                            )
                        )
                        return@patch
                    }

                    // Update the bank account
                    val updatedAccount = BankAccountMapper.updateRequestToDomain(existingAccount, request)
                    val result = bankAccountRepository.updateBankAccount(accountNumber, updatedAccount)

                    if (result != null) {
                        logger.info("Successfully updated bank account with account number: $accountNumber")
                        call.respond(HttpStatusCode.OK, BankAccountMapper.toBankAccountResponse(result))
                    } else {
                        logger.warn("Failed to update bank account with account number: $accountNumber")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("message" to "Failed to update bank account")
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update bank account", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "Failed to update bank account: ${e.message}")
                    )
                }
            }

            // DELETE endpoint to delete a bank account
            delete("{accountNumber}") {
                val accountNumber = call.parameters["accountNumber"]
                logger.info("Received delete bank account request for account number: $accountNumber")

                if (accountNumber == null) {
                    logger.warn("Account number is null")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Account number is required"))
                    return@delete
                }

                val validationError = validationService.validateAccountNumber(accountNumber)
                if (validationError != null) {
                    logger.warn("Invalid account number format: $accountNumber")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "message" to "Validation failed",
                            "errors" to listOf(validationError)
                        )
                    )
                    return@delete
                }

                try {
                    // Get the existing bank account
                    val existingAccount = bankAccountRepository.getBankAccountByAccountNumber(accountNumber)
                    if (existingAccount == null) {
                        logger.warn("Bank account not found with account number: $accountNumber")
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Bank account not found"))
                        return@delete
                    }

                    // Check if the user has permission to delete this bank account
                    val userId = RoleBasedAuthorization.getUserId(call)
                    val isOwnAccount = userId == existingAccount.userId

                    if (!isOwnAccount) {
                        logger.warn("Access denied: User $userId attempted to delete bank account $accountNumber")
                        call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("message" to "Access denied: Insufficient permissions")
                        )
                        return@delete
                    }

                    // Delete the bank account
                    val result = bankAccountRepository.deleteBankAccount(accountNumber)
                    if (result) {
                        logger.info("Successfully deleted bank account with account number: $accountNumber")
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        logger.warn("Failed to delete bank account with account number: $accountNumber")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("message" to "Failed to delete bank account")
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete bank account", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "Failed to delete bank account: ${e.message}")
                    )
                }
            }
        }
    }
}
