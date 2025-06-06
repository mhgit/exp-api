package com.eaglebank.api.presentation.route

import com.eaglebank.api.infra.persistence.UserRepository
import com.eaglebank.api.infra.validation.SimpleUserRequestValidationService
import com.eaglebank.api.presentation.dto.ValidationDetail
import com.eaglebank.api.presentation.dto.ValidationType
import com.eaglebank.api.presentation.dto.BadRequestErrorResponse
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UpdateUserRequest
import com.eaglebank.api.presentation.dto.UserResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.UUID

fun Route.usersRoute() {
    val validationService: SimpleUserRequestValidationService by inject()
    val userRepository: UserRepository by inject() // Inject UserRepository

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

                // Call the UserRepository to create the user
                val createdUser = userRepository.createUser(request)
                
                call.respond(HttpStatusCode.Created, createdUser)
            } catch (e: Exception) { // Catch specific exceptions if possible for more granular error handling
                call.respond(
                    HttpStatusCode.InternalServerError, // Changed to InternalServerError as database errors are server-side
                    mapOf("message" to "Failed to create user: ${e.message}") // Include error message for debugging
                )
            }
        }
        // Add routes for GET, PUT, DELETE for users here
        get("{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                return@get
            }
            val user = userRepository.getUserById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
            } else {
                call.respond(HttpStatusCode.OK, user)
            }
        }

        put("{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                return@put
            }
            val request = call.receive<UpdateUserRequest>()
            val updatedUser = userRepository.updateUser(id, request)
            if (updatedUser == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found or update failed"))
            } else {
                call.respond(HttpStatusCode.OK, updatedUser)
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                return@delete
            }
            val deleted = userRepository.deleteUser(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found or delete failed"))
            }
        }

        get {
            val allUsers = userRepository.getAllUsers()
            call.respond(HttpStatusCode.OK, allUsers)
        }
    }
}