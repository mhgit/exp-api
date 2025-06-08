package com.eaglebank.api.presentation.route

import com.eaglebank.api.domain.repository.IUserRepository
import com.eaglebank.api.infra.security.RoleBasedAuthorization
import com.eaglebank.api.infra.security.withRole
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
        // All endpoints are protected with JWT authentication
        authenticate("auth-jwt") {
            // POST endpoint is protected - only admin can create users
            withRole("admin") {
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
            }

            // GET user by ID - User can fetch their own user, admin and account manager can fetch any user
            get("{id}") {
                val id = call.parameters["id"]
                logger.info("Received get user request for ID: $id")

                if (id == null || !id.matches(Regex("^usr-[A-Za-z0-9]+$"))) {
                    logger.warn("Invalid user ID format: $id")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                    return@get
                }

                // Get the user from the repository
                val user = userRepository.getUserById(id)
                if (user == null) {
                    logger.warn("User not found with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
                    return@get
                }

                // Check if the user has permission to access this user
                val currentUserId = RoleBasedAuthorization.getUserId(call)
                val principal = call.principal<JWTPrincipal>()
                val hasAdminRole = RoleBasedAuthorization.hasRole(principal, "admin")
                val hasAccountManagerRole = RoleBasedAuthorization.hasRole(principal, "account-manager")
                val isOwnUser = currentUserId == user.keycloakId

                if (isOwnUser || hasAdminRole || hasAccountManagerRole) {
                    logger.debug("Successfully retrieved user with ID: $id")
                    call.respond(HttpStatusCode.OK, UserMapper.toUserResponse(user))
                } else {
                    logger.warn("Access denied: User $currentUserId attempted to access user $id")
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("message" to "Access denied: Insufficient permissions")
                    )
                }
            }

            // PUT user by ID - User can update their own user, admin and account manager can update any user
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

                // Check if the user has permission to update this user
                val currentUserId = RoleBasedAuthorization.getUserId(call)
                val principal = call.principal<JWTPrincipal>()
                val hasAdminRole = RoleBasedAuthorization.hasRole(principal, "admin")
                val hasAccountManagerRole = RoleBasedAuthorization.hasRole(principal, "account-manager")
                val isOwnUser = currentUserId == existingUser.keycloakId

                if (!isOwnUser && !hasAdminRole && !hasAccountManagerRole) {
                    logger.warn("Access denied: User $currentUserId attempted to update user $id")
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("message" to "Access denied: Insufficient permissions")
                    )
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

            // DELETE user by ID - Only admin can delete users
            delete("{id}") {
                val id = call.parameters["id"]
                logger.info("Received delete user request for ID: $id")

                if (id == null || !id.matches(Regex("^usr-[A-Za-z0-9]+$"))) {
                    logger.warn("Invalid user ID format: $id")
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid user ID format"))
                    return@delete
                }

                // Check if the user has the admin role FIRST before checking if the user exists
                // This prevents information leakage about user existence
                val principal = call.principal<JWTPrincipal>()
                val hasAdminRole = RoleBasedAuthorization.hasRole(principal, "admin")
                if (!hasAdminRole) {
                    logger.warn("Access denied: User does not have required role: admin")
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("message" to "Access denied: Insufficient permissions")
                    )
                    return@delete
                }

                // Only check if the user exists AFTER confirming the requester has admin role
                val existingUser = userRepository.getUserById(id)
                if (existingUser == null) {
                    logger.warn("User not found with ID: $id")
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
                    return@delete
                }

                if (userRepository.deleteUser(id)) {
                    logger.info("Successfully deleted user with ID: $id")
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    logger.warn("Failed to delete user with ID: $id")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to delete user"))
                }
            }

            // GET all users - Available to all authenticated users
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
