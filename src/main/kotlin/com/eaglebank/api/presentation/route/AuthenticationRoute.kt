package com.eaglebank.api.presentation.route

import com.eaglebank.api.config.KeycloakConfig
import com.eaglebank.api.presentation.dto.AuthRequest
import com.eaglebank.api.presentation.dto.AuthResponse
import com.eaglebank.api.presentation.dto.RefreshTokenResponse
import com.eaglebank.api.presentation.dto.TokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.authenticationRoutes() {
    val logger = LoggerFactory.getLogger("AuthenticationRoute")
    val keycloakConfig: KeycloakConfig by inject()

    post("/login") {
        try {
            val request = call.receive<AuthRequest>()
            logger.debug("Processing login request for username: ${request.username}")

            // Create form parameters for Keycloak token request
            val formParameters = Parameters.build {
                append("client_id", keycloakConfig.clientId)
                append("client_secret", keycloakConfig.clientSecret)
                append("grant_type", "password")
                append("username", request.username)
                append("password", request.password)
            }

            // Make request to Keycloak token endpoint
            val client = HttpClient(CIO)

            val tokenEndpoint =
                "${keycloakConfig.serverUrl}/realms/${keycloakConfig.realm}/protocol/openid-connect/token"
            val response = client.submitForm(
                url = tokenEndpoint,
                formParameters = formParameters
            )

            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                val tokenInfo = Json.decodeFromString<TokenResponse>(responseBody)

                // Set refresh token as HTTP-only cookie
                tokenInfo.refreshToken?.let { refreshToken ->
                    call.response.cookies.append(
                        Cookie(
                            name = "refreshToken",
                            value = refreshToken,
                            httpOnly = true,
                            path = "/",
                            maxAge = 30 * 24 * 60 * 60 // 30 days
                        )
                    )
                }

                // Return access token in response body
                call.respond(
                    HttpStatusCode.OK, AuthResponse(
                        accessToken = tokenInfo.accessToken
                    )
                )

                logger.info("Successfully authenticated user: ${request.username}")
            } else {
                logger.warn("Authentication failed for user: ${request.username}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Invalid credentials"))
            }

            client.close()
        } catch (e: Exception) {
            logger.error("Error during authentication", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Authentication failed"))
        }
    }

    post("/refresh-token") {
        try {
            val refreshToken = call.request.cookies["refreshToken"]
            if (refreshToken == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Refresh token not found"))
                return@post
            }

            // Create form parameters for Keycloak token refresh request
            val formParameters = Parameters.build {
                append("client_id", keycloakConfig.clientId)
                append("client_secret", keycloakConfig.clientSecret)
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }

            // Make request to Keycloak token endpoint
            val client = HttpClient(CIO)

            val tokenEndpoint =
                "${keycloakConfig.serverUrl}/realms/${keycloakConfig.realm}/protocol/openid-connect/token"
            val response = client.submitForm(
                url = tokenEndpoint,
                formParameters = formParameters
            )

            if (response.status.isSuccess()) {
                val responseBody = response.body<String>()
                val tokenInfo = Json.decodeFromString<TokenResponse>(responseBody)

                // Update refresh token cookie if a new one is provided
                tokenInfo.refreshToken?.let { newRefreshToken ->
                    call.response.cookies.append(
                        Cookie(
                            name = "refreshToken",
                            value = newRefreshToken,
                            httpOnly = true,
                            path = "/",
                            maxAge = 30 * 24 * 60 * 60 // 30 days
                        )
                    )
                }

                // Return new access token in response body
                call.respond(
                    HttpStatusCode.OK, RefreshTokenResponse(
                        accessToken = tokenInfo.accessToken
                    )
                )

                logger.info("Successfully refreshed token")
            } else {
                logger.warn("Token refresh failed")
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Invalid or expired refresh token"))
            }

            client.close()
        } catch (e: Exception) {
            logger.error("Error during token refresh", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Token refresh failed"))
        }
    }

    authenticate("auth-jwt") {
        get("/protected") {
            try {
                val principal = call.principal<JWTPrincipal>()
                if (principal != null) {
                    val subject = principal.subject
                    val preferredUsername = principal.payload.getClaim("preferred_username").asString()

                    call.respondText("Hello, $preferredUsername! Your user ID is $subject. You accessed a protected route.")
                    logger.info("Protected resource accessed by user: $preferredUsername")
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "No valid principal found"))
                    logger.warn("Protected resource access attempt with no valid principal")
                }
            } catch (e: Exception) {
                logger.error("Error accessing protected resource", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("message" to "Error accessing protected resource")
                )
            }
        }
    }
}
