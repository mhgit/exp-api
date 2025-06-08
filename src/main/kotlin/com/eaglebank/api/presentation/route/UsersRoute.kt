package com.eaglebank.api.presentation.route

import com.eaglebank.api.domain.repository.IUserRepository
import com.eaglebank.api.infra.validation.UserRequestValidationService
import com.eaglebank.api.presentation.dto.CreateUserRequest
import com.eaglebank.api.presentation.dto.UpdateUserRequest
import com.eaglebank.api.presentation.mapper.UserMapper
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.usersRoute() {
    val logger = LoggerFactory.getLogger("UsersRoute")
    val userRepository: IUserRepository by inject()
    val validationService: UserRequestValidationService by inject()

    route("/v1/users") {
        // POST endpoint is intentionally not protected.  We would design some kind of keycloak flow perhaps.
        // i.e. an approved user is given a short term link to the app, which allows them to create the user.
        post {
            logger.info("Received create user request")
            try {
                val request = call.receive<CreateUserRequest>()
                logger.debug("Processing create user request for email: ${request.email}")

                // Validate the request
                val validationErrors = validationService.validateCreateUserRequest(request)
                if (validationErrors.isNotEmpty()) {
                    logger.warn("Validation failed for create user request: $validationErrors")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "message" to "Validation failed",
                            "errors" to validationErrors
                        )
                    )
                    return@post
                }

                val domainUser = UserMapper.createRequestToDomain(request)
                val createdUser = userRepository.createUser(domainUser)
                logger.info("Successfully created user with ID: ${createdUser.id}")

                call.respond(HttpStatusCode.Created, UserMapper.toUserResponse(createdUser))
            } catch (e: Exception) {
                logger.error("Failed to create user", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("message" to "Failed to create user: ${e.message}")
                )
            }
        }

        // All other endpoints are protected with JWT authentication
        authenticate("auth-jwt") {
            get("{id}") {
                val id = call.parameters["id"]
                logger.info("Received get user request for ID: $id")

                if (id == null || !id.matches(Regex("^usr-[A-Za-z0-9]+$"))) {
                    logger.warn("Invalid user ID format: $id")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                    return@get
                }

                userRepository.getUserById(id)?.let { user ->
                    logger.debug("Successfully retrieved user with ID: $id")
                    call.respond(HttpStatusCode.OK, UserMapper.toUserResponse(user))
                } ?: run {
                    logger.warn("User not found with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
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

                val existingUser = userRepository.getUserById(id)
                if (existingUser == null) {
                    logger.warn("User not found with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
                    return@put
                }

                val request = call.receive<UpdateUserRequest>()
                val domainUser = UserMapper.updateRequestToDomain(id, existingUser, request)
                val updatedUser = userRepository.updateUser(id, domainUser)

                if (updatedUser != null) {
                    logger.info("Successfully updated user with ID: $id")
                    call.respond(HttpStatusCode.OK, UserMapper.toUserResponse(updatedUser))
                } else {
                    logger.warn("Failed to update user with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found or update failed"))
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

                if (userRepository.deleteUser(id)) {
                    logger.info("Successfully deleted user with ID: $id")
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    logger.warn("Failed to delete user with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found or delete failed"))
                }
            }

            get {
                logger.info("Received get all users request")
                try {
                    // Log authentication information for debugging
                    val principal = call.principal<JWTPrincipal>()
                    if (principal != null) {
                        logger.debug("Authenticated request from subject: ${principal.subject}")
                        logger.debug("Token claims: ${principal.payload.claims}")
                    } else {
                        logger.warn("No JWT principal found in authenticated request")
                    }

                    val allUsers = userRepository.getAllUsers()
                        .map(UserMapper::toUserResponse)
                    logger.debug("Retrieved ${allUsers.size} users")
                    call.respond(HttpStatusCode.OK, allUsers)
                } catch (e: Exception) {
                    logger.error("Error processing get all users request", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "Failed to retrieve users: ${e.message}")
                    )
                }
            }
        }
    }
}
