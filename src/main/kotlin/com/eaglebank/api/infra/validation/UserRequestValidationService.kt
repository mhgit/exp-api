
package com.eaglebank.api.infra.validation

import com.eaglebank.api.presentation.dto.Address
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.ValidationDetail

fun String.validateRequired(fieldName: String): ValidationDetail? =
    if (isBlank()) ValidationDetail.required(fieldName)
    else null

class SimpleUserRequestValidationService : UserRequestValidationService {
    override fun validateCreateUserRequest(request: CreateUserRequest): List<ValidationDetail> {
        return buildList {
            request.name.validateRequired("name")?.let { add(it) }

            if (!isValidEmail(request.email)) {
                add(ValidationDetail.invalidFormat("email"))
            }

            if (!isValidPhoneNumber(request.phoneNumber)) {
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

            if (!isValidPostcode(address.postcode)) {
                add(ValidationDetail.invalidFormat("address.postcode"))
            }
        }.takeIf { it.isNotEmpty() }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex(
            "^[A-Za-z0-9]+[A-Za-z0-9._-]*[A-Za-z0-9]+@[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]\\.[A-Za-z]{2,}$"
        )
        return email.matches(emailRegex)
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = Regex("^\\+\\d{2}-\\d{4}-\\d{4}$")
        return phoneNumber.matches(phoneRegex)
    }

    private fun isValidPostcode(postcode: String): Boolean {
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
