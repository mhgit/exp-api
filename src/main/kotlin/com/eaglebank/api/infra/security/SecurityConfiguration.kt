package com.eaglebank.api.infra.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.eaglebank.api.presentation.dto.AuthRequest
import com.eaglebank.api.presentation.dto.AuthResponse
import com.eaglebank.api.presentation.dto.ErrorResponse
import com.eaglebank.api.presentation.dto.RefreshTokenResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.config.* // Import ApplicationConfig

// Change the parameter type to io.ktor.server.config.ApplicationConfig
fun Application.configureSecurity(config: ApplicationConfig) {
    // Correctly access configuration values using config.property("path").getString()
    val jwtRealm = config.property("jwt.realm").getString()
    val jwtSecret = config.property("jwt.secret").getString()
    val jwtIssuer = config.property("jwt.issuer").getString()
    val jwtAudience = config.property("jwt.audience").getString()
    val jwtAccessTokenExpiration = config.property("jwt.accessToken.expiration").getString().toLong()
    val jwtRefreshTokenExpiration = config.property("jwt.refreshToken.expiration").getString().toLong()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Token is not valid or has expired")
                )
            }
        }
    }

    routing {
        post("/login") {
            val authRequest = call.receive<AuthRequest>()
            // TODO: Implement actual authentication logic
            val token = createJWTToken(
                jwtIssuer,
                jwtAudience,
                jwtSecret,
                jwtAccessTokenExpiration
            )
            val refreshToken = createRefreshToken(
                jwtIssuer,
                jwtAudience,
                jwtSecret,
                jwtRefreshTokenExpiration
            )

            call.response.cookies.append(
                Cookie(
                    name = "refreshToken",
                    value = refreshToken,
                    encoding = CookieEncoding.URI_ENCODING,
                    maxAge = (jwtRefreshTokenExpiration / 1000).toInt(),
                    path = "/",
                    httpOnly = true,
                    secure = true,
                    extensions = mapOf("SameSite" to "Lax")
                )
            )

            call.respond(AuthResponse(token))
        }

        post("/refresh-token") {
            val refreshToken = call.request.cookies["refreshToken"]
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Refresh token not found"))

            try {
                // Verify refresh token
                val verifier = JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()

                val decodedJWT = verifier.verify(refreshToken)

                if (decodedJWT.getClaim("type").asString() != "refresh") {
                    return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token type"))
                }

                // Generate new access token
                val newToken = createJWTToken(
                    jwtIssuer,
                    jwtAudience,
                    jwtSecret,
                    jwtAccessTokenExpiration,
                    decodedJWT.subject
                )

                call.respond(RefreshTokenResponse(newToken))
            } catch (_: Exception) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired refresh token"))
            }
        }
    }
}

fun createJWTToken(issuer: String, audience: String, secret: String, expirationMillis: Long, subject: String? = null): String {
    return JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .apply { subject?.let { withSubject(it) } }
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + expirationMillis))
        .sign(Algorithm.HMAC256(secret))
}

fun createRefreshToken(issuer: String, audience: String, secret: String, expirationMillis: Long): String {
    return JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("type", "refresh")
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + expirationMillis))
        .sign(Algorithm.HMAC256(secret))
}