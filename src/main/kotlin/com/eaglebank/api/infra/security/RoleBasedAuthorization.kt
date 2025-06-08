package com.eaglebank.api.infra.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Utility class for role-based authorization in the application.
 */
object RoleBasedAuthorization {
    private val logger = LoggerFactory.getLogger(RoleBasedAuthorization::class.java)

    /**
     * Checks if the current user has the specified role.
     *
     * @param call The ApplicationCall containing the JWT principal
     * @param role The role to check for
     * @return true if the user has the role, false otherwise
     */
    fun hasRole(call: ApplicationCall, role: String): Boolean {
        val principal = call.principal<JWTPrincipal>() ?: return false
        return hasRole(principal, role)
    }

    /**
     * Checks if the JWT principal has the specified role.
     *
     * @param principal The JWT principal
     * @param role The role to check for
     * @return true if the principal has the role, false otherwise
     */
    fun hasRole(principal: JWTPrincipal?, role: String): Boolean {
        if (principal == null) return false

        try {
            val realmAccess = principal.payload.getClaim("realm_access")
            if (realmAccess.isNull) {
                logger.warn("realm_access claim is null in JWT token")
                return false
            }

            val roles = realmAccess.asMap()?.get("roles") as? List<*>
            if (roles == null) {
                logger.warn("roles not found in realm_access claim")
                return false
            }

            return roles.contains(role)
        } catch (e: Exception) {
            logger.error("Error checking role: ${e.message}", e)
            return false
        }
    }

    /**
     * Checks if the current user has any of the specified roles.
     *
     * @param call The ApplicationCall containing the JWT principal
     * @param roles The roles to check for
     * @return true if the user has any of the roles, false otherwise
     */
    fun hasAnyRole(call: ApplicationCall, vararg roles: String): Boolean {
        val principal = call.principal<JWTPrincipal>() ?: return false
        return hasAnyRole(principal, *roles)
    }

    /**
     * Checks if the JWT principal has any of the specified roles.
     *
     * @param principal The JWT principal
     * @param roles The roles to check for
     * @return true if the principal has any of the roles, false otherwise
     */
    fun hasAnyRole(principal: JWTPrincipal?, vararg roles: String): Boolean {
        if (principal == null) return false

        return roles.any { hasRole(principal, it) }
    }

    /**
     * Gets the user ID from the JWT token.
     *
     * @param call The ApplicationCall containing the JWT principal
     * @return The user ID or null if not found
     */
    fun getUserId(call: ApplicationCall): String? {
        val principal = call.principal<JWTPrincipal>() ?: return null
        return principal.subject
    }
}

/**
 * Extension function for Route to add role-based authorization.
 * Only users with the specified role can access the route.
 *
 * @param role The required role
 * @param build The route builder
 * @return The created route
 */
fun Route.withRole(role: String, build: Route.() -> Unit): Route {
    val logger = LoggerFactory.getLogger("RoleBasedAuthorization")

    val route = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
            RouteSelectorEvaluation.Constant
    })

    route.intercept(ApplicationCallPipeline.Plugins) {
        if (!RoleBasedAuthorization.hasRole(call, role)) {
            logger.warn("Access denied: User does not have required role: $role")
            call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Access denied: Insufficient permissions"))
            return@intercept finish()
        }
    }

    route.build()
    return route
}

/**
 * Extension function for Route to add role-based authorization.
 * Only users with any of the specified roles can access the route.
 *
 * @param roles The required roles (any of them)
 * @param build The route builder
 * @return The created route
 */
fun Route.withAnyRole(vararg roles: String, build: Route.() -> Unit): Route {
    val logger = LoggerFactory.getLogger("RoleBasedAuthorization")

    val route = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
            RouteSelectorEvaluation.Constant
    })

    route.intercept(ApplicationCallPipeline.Plugins) {
        if (!RoleBasedAuthorization.hasAnyRole(call, *roles)) {
            logger.warn("Access denied: User does not have any of the required roles: ${roles.joinToString()}")
            call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Access denied: Insufficient permissions"))
            return@intercept finish()
        }
    }

    route.build()
    return route
}
