
package com.eaglebank.api.presentation.route

import com.eaglebank.api.infra.validation.SimpleUserRequestValidationService

import com.eaglebank.api.presentation.dto.BadRequestErrorResponse
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UserResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

import java.time.Instant
import java.util.*

fun Route.usersRoute() {
    val validationService: SimpleUserRequestValidationService by inject()


    route("/v1/users") {
        post {
            try {
                val request = call.receive<CreateUserRequest>()

                // Validate the request
                val validationErrors = validationService.validateCreateUserRequest(request)
                if (validationErrors.isNotEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        BadRequestErrorResponse(
                            message = "Validation failed",
                            details = validationErrors
                        )
                    )
                    return@post
                }

                // TODO: Add actual user creation logic through a service
                
                // This is a temporary response for demonstration
                val response = UserResponse(
                    id = UUID.randomUUID().toString(),
                    name = request.name,
                    address = request.address,
                    phoneNumber = request.phoneNumber,
                    email = request.email,
                    createdTimestamp = Instant.now().toString(),
                    updatedTimestamp = Instant.now().toString()
                )
                
                call.respond(HttpStatusCode.Created, response)
            } catch (_: Exception) {
                // TODO: Add proper error handling
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Failed to create user")
                )
            }
        }
    }
}