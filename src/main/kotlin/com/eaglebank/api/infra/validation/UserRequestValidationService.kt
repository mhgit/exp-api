
package com.eaglebank.api.infra.validation
/*
Note for Reviewer.  This is a very basic validation system.  Validation is notorious for:
- Hard to finish the solution.  Everytime you think you are finished, along comes another use case that slips through.
- This in turn can lead to validation packages that are hard to maintain.
- Performance issues creep in.
- Maintenance of regex used below adds another language, complexity and debugging issues.
- Tests themselves become over complex.

So I am not going to pretend this impl covers it all but it's a good place to start a conversation that might cover some of these points:

Core Improvements:
- Add configurable validation rules (min/max lengths, allowed characters)
- Extend name validation (length and character checks)
- Improve email validation (check for disposable domains)
- Add international phone number format support
- Enhance address validation (line lengths, valid counties)

Technical Improvement Ideas:
- Is there an annotation library we could use in the DTO class?  If so do we like cluttering DTO with validations?
- Create a validation DSL for better readability
- Add support for async validations (e.g., checking if email exists)
- Implement validation caching for expensive operations
- Add validation groups (CREATE, UPDATE, DELETE)
- Allow custom validation rules to be plugged in

Testing Improvement Ideas:
- Add a testing DSL for validation scenarios
- Create more comprehensive test cases, mine need more real world cases
-- Could generate from AI analysis of real data examples perhaps?
- Add performance tests for validation operations

Documentation Improvements:
- Document all validation rules clearly
- Add code examples for common validation scenarios
- Include validation error message templates
- Document expected formats for all fields

To be fair....thats just the tip of the iceburg for validation.  Suggest a brain storm with the team with this as
a good foundation for discussion.  :-)
 */
import com.eaglebank.api.presentation.dto.Address
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.ValidationDetail

fun String.validateRequired(fieldName: String): ValidationDetail? =
    if (isBlank()) ValidationDetail.required(fieldName)
    else null

/**
 * A basic implementation of [UserRequestValidationService] that performs validation
 * of user-related requests according to predefined rules and patterns.
 *
 * This service validates:
 * - Required fields (non-blank values)
 * - Email format
 * - Phone number format (+XX-XXXX-XXXX)
 * - UK postal code format
 * - Address components
 *
 * Example usage:
 * ```kotlin
 * val validationService = SimpleUserRequestValidationService()
 * val request = CreateUserRequest(
 *     name = "John Doe",
 *     email = "john.doe@example.com",
 *     phoneNumber = "+44-1234-5678",
 *     address = Address(...)
 * )
 * val validationErrors = validationService.validateCreateUserRequest(request)
 * if (validationErrors.isEmpty()) {
 *     // Request is valid
 * }
 * ```
 *
 * The service implements a fail-fast approach, collecting all validation errors
 * rather than stopping at the first encountered error.
 */
class SimpleUserRequestValidationService : UserRequestValidationService {
    companion object {
        private object ValidationPatterns {
            /**
             * A Regex pattern used to validate email addresses.
             *
             * Rules for validation:
             * - The local part (before the `@`) must begin and end with an alphanumeric character.
             * - The local part can include alphanumeric characters, dots (`.`), underscores (`_`), and hyphens (`-`).
             * - The domain part (after the `@`) must begin and end with an alphanumeric character.
             * - The domain part can include alphanumeric characters, dots (`.`), and hyphens (`-`), but cannot have consecutive dots.
             * - The domain must contain a valid top-level domain (TLD) that is at least two characters long.
             *
             * This pattern ensures that the email is structurally valid but does not guarantee the email's existence or validity of the domain.
             */
            val EMAIL = Regex(
                "^[A-Za-z0-9]+[A-Za-z0-9._-]*[A-Za-z0-9]+@" +
                        "[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]\\.[A-Za-z]{2,}$"
            )
            /**
             * Regular expression for validating phone numbers in the format "+XX-XXXX-XXXX".
             * The format specifies:
             * - A plus sign (+) at the beginning.
             * - Two digits representing the country code (XX).
             * - A hyphen (-).
             * - Four digits (XXXX), representing the first part of the phone number.
             * - Another hyphen (-).
             * - Four more digits (XXXX), representing the second part of the phone number.
             *
             * Used to ensure that the phone number matches a specific standardized pattern.
             */
            val PHONE = Regex("^\\+\\d{2}-\\d{4}-\\d{4}$")
            /**
             * Regular expression for validating UK postcodes.
             *
             * The format enforces a specific structure:
             * - Begins with one or two uppercase letters representing the postal area.
             * - Followed by one or two digits and optionally an additional uppercase letter or digit.
             * - Contains an optional space separating the outward code from the inward code.
             * - Ends with a digit and two uppercase letters representing the inward code.
             *
             * This pattern ensures UK postcode validity in accordance with standard formats.
             */
            val POSTCODE = Regex("^[A-Z]{1,2}[0-9][A-Z0-9]? ?[0-9][A-Z]{2}$")
        }
    }

    override fun validateCreateUserRequest(request: CreateUserRequest): List<ValidationDetail> {
        return buildList {
            request.name.validateRequired("name")?.let { add(it) }

            if (!request.email.matches(ValidationPatterns.EMAIL)) {
                add(ValidationDetail.invalidFormat("email"))
            }

            if (!request.phoneNumber.matches(ValidationPatterns.PHONE)) {
                add(ValidationDetail.invalidFormat("phoneNumber",
                    "Must be in format: +XX-XXXX-XXXX"))
            }

            validateAddress(request.address)?.let { addAll(it) }
        }
    }

    private fun validateAddress(address: Address): List<ValidationDetail>? {
        return buildList {
            address.line1.validateRequired("address.line1")?.let { add(it) }
            address.town.validateRequired("address.town")?.let { add(it) }
            address.county.validateRequired("address.county")?.let { add(it) }

            if (!address.postcode.uppercase().matches(ValidationPatterns.POSTCODE)) {
                add(ValidationDetail.invalidFormat("address.postcode"))
            }
        }.takeIf { it.isNotEmpty() }
    }

}


interface UserRequestValidationService {
    /**
     * Validates a CreateUserRequest and returns a list of validation details.
     * An empty list indicates no validation errors were found.
     *
     * @param request The CreateUserRequest to validate
     * @return List of ValidationDetail objects describing any validation errors
     */
    fun validateCreateUserRequest(request: CreateUserRequest): List<ValidationDetail>
}
