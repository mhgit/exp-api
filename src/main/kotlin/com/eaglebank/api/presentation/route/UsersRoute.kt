package com.eaglebank.api.presentation.route

import com.eaglebank.api.infra.persistence.UserRepository
import com.eaglebank.api.infra.validation.SimpleUserRequestValidationService
import com.eaglebank.api.presentation.dto.BadRequestErrorResponse
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UpdateUserRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.usersRoute() {
    val logger = LoggerFactory.getLogger("UsersRoute")
    val validationService: SimpleUserRequestValidationService by inject()
    val userRepository: UserRepository by inject()

    route("/v1/users") {
        post {
            logger.info("Received create user request")
            try {
                val request = call.receive<CreateUserRequest>()
                logger.debug("Processing create user request for email: ${request.email}")

                val validationErrors = validationService.validateCreateUserRequest(request)
                if (validationErrors.isNotEmpty()) {
                    logger.warn("Validation failed for create user request: $validationErrors")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        BadRequestErrorResponse(
                            message = "Validation failed",
                            details = validationErrors
                        )
                    )
                    return@post
                }

                val createdUser = userRepository.createUser(request)
                logger.info("Successfully created user with ID: ${createdUser.id}")
                
                call.respond(HttpStatusCode.Created, createdUser)
            } catch (e: Exception) {
                logger.error("Failed to create user", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("message" to "Failed to create user: ${e.message}")
                )
            }
        }

        authenticate("auth-jwt") {
            get("{id}") {
                val id = call.parameters["id"]
                logger.info("Received get user request for ID: $id")
                
                if (id == null || !id.matches(Regex("^usr-[A-Za-z0-9]+$"))) {
                    logger.warn("Invalid user ID format: $id")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                    return@get
                }
                
                val user = userRepository.getUserById(id)
                if (user == null) {
                    logger.warn("User not found with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
                } else {
                    logger.debug("Successfully retrieved user with ID: $id")
                    call.respond(HttpStatusCode.OK, user)
                }
            }

            put("{id}") {
                val id = call.parameters["id"]
                logger.info("Received update user request for ID: $id")
                
                if (id == null || !id.matches(Regex("^usr-[A-Za-z0-9]+$"))) {
                    logger.warn("Invalid user ID format: $id")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                    return@put
                }
                
                val request = call.receive<UpdateUserRequest>()
                val updatedUser = userRepository.updateUser(id, request)
                if (updatedUser == null) {
                    logger.warn("Failed to update user with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found or update failed"))
                } else {
                    logger.info("Successfully updated user with ID: $id")
                    call.respond(HttpStatusCode.OK, updatedUser)
                }
            }

            delete("{id}") {
                val id = call.parameters["id"]
                logger.info("Received delete user request for ID: $id")
                
                if (id == null || !id.matches(Regex("^usr-[A-Za-z0-9]+$"))) {
                    logger.warn("Invalid user ID format: $id")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                    return@delete
                }
                
                val deleted = userRepository.deleteUser(id)
                if (deleted) {
                    logger.info("Successfully deleted user with ID: $id")
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    logger.warn("Failed to delete user with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found or delete failed"))
                }
            }

            get {
                logger.info("Received get all users request")
                val allUsers = userRepository.getAllUsers()
                logger.debug("Retrieved ${allUsers.size} users")
                call.respond(HttpStatusCode.OK, allUsers)
            }
        }
    }
}