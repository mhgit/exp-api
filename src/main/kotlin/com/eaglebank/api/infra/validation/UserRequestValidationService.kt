
package com.eaglebank.api.infra.validation

import com.eaglebank.api.presentation.dto.Address
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.ValidationDetail

class SimpleUserRequestValidationService : UserRequestValidationService
{
    override fun validateCreateUserRequest(request: CreateUserRequest): List<ValidationDetail> {
        val validationErrors = mutableListOf<ValidationDetail>()

        // Name validation
        if (request.name.isBlank()) {
            validationErrors.add(
                ValidationDetail(
                    field = "name",
                    message = "Name cannot be empty",
                    type = "REQUIRED_FIELD"
                )
            )
        }

        // Email validation
        if (!isValidEmail(request.email)) {
            validationErrors.add(
                ValidationDetail(
                    field = "email",
                    message = "Invalid email format",
                    type = "INVALID_FORMAT"
                )
            )
        }

        // Phone number validation
        if (!isValidPhoneNumber(request.phoneNumber)) {
            validationErrors.add(
                ValidationDetail(
                    field = "phoneNumber",
                    message = "Invalid phone number format. Must be in format: +XX-XXXX-XXXX",
                    type = "INVALID_FORMAT"
                )
            )
        }

        // Address validation
        validateAddress(request.address)?.forEach { validationErrors.add(it) }

        return validationErrors
    }

    private fun validateAddress(address: Address): List<ValidationDetail>? {
        val validationErrors = mutableListOf<ValidationDetail>()

        if (address.line1.isBlank()) {
            validationErrors.add(
                ValidationDetail(
                    field = "address.line1",
                    message = "Address line 1 cannot be empty",
                    type = "REQUIRED_FIELD"
                )
            )
        }

        if (address.town.isBlank()) {
            validationErrors.add(
                ValidationDetail(
                    field = "address.town",
                    message = "Town cannot be empty",
                    type = "REQUIRED_FIELD"
                )
            )
        }

        if (address.county.isBlank()) {
            validationErrors.add(
                ValidationDetail(
                    field = "address.county",
                    message = "County cannot be empty",
                    type = "REQUIRED_FIELD"
                )
            )
        }

        if (!isValidPostcode(address.postcode)) {
            validationErrors.add(
                ValidationDetail(
                    field = "address.postcode",
                    message = "Invalid postcode format",
                    type = "INVALID_FORMAT"
                )
            )
        }

        return if (validationErrors.isEmpty()) null else validationErrors
    }

    private fun isValidEmail(email: String): Boolean {
        // This regex pattern checks for:
        // 1. Local part before @ (alphanumeric, dots, underscores, hyphens)
        // 2. @ symbol
        // 3. Domain part (at least one dot, valid characters)
        val emailRegex = Regex(
            "^[A-Za-z0-9]+[A-Za-z0-9._-]*[A-Za-z0-9]+@[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]\\.[A-Za-z]{2,}$"
        )
        /*
        - `^[A-Za-z0-9]+` - Start with at least one alphanumeric character
        - `[A-Za-z0-9._-]*` - Followed by any number of alphanumeric characters, dots, underscores, or hyphens
        - `[A-Za-z0-9]+@` - End local part with alphanumeric and @ symbol
        - `[A-Za-z0-9][A-Za-z0-9.-]*` - Domain part starts with alphanumeric, followed by optional alphanumeric, dots, or hyphens
        - `[A-Za-z0-9]\\.[A-Za-z]{2,}$` - Domain must end with alphanumeric, dot, and at least 2 letters

         */
        return email.matches(emailRegex)
    }


    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = Regex("^\\+\\d{2}-\\d{4}-\\d{4}$")
        return phoneNumber.matches(phoneRegex)
    }

    private fun isValidPostcode(postcode: String): Boolean {
        // UK postcode format: AA9A 9AA, A9A 9AA, A9 9AA, A99 9AA, AA9 9AA, AA99 9AA
        val postcodeRegex = Regex("^[A-Z]{1,2}[0-9][A-Z0-9]? ?[0-9][A-Z]{2}$")
        return postcode.uppercase().matches(postcodeRegex)
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
