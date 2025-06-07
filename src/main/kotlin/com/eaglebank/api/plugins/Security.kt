package com.eaglebank.api.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*

private val logger = LoggerFactory.getLogger("SecurityPlugin")

fun Application.configureSecurity() {
    val config = environment.config
    val keycloakRealm = config.property("keycloak.realm").getString()
    val keycloakUrl = config.property("keycloak.serverUrl").getString()
    val keycloakIssuer = "$keycloakUrl/realms/$keycloakRealm"

    authentication {
        jwt("auth-jwt") {
            verifier(
                JWT
                    .require(Algorithm.RSA256(getPublicKey(keycloakIssuer), null))
                    .withIssuer(keycloakIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                throw AuthenticationException("Token is not valid or has expired")
            }
        }
    }
}

private suspend fun getPublicKey(issuerUrl: String): PublicKey {
    val wellKnownUrl = "$issuerUrl/.well-known/openid-configuration"
    val client = HttpClient(CIO)

    return try {
        val wellKnown: JsonObject = client.get(wellKnownUrl).body()
        val jwksUri = wellKnown["jwks_uri"].toString().removeSurrounding("\"")
        val jwks: JsonObject = client.get(jwksUri).body()

        val keyContent = jwks["keys"]?.jsonArray
            ?.first { it.jsonObject["use"]?.jsonPrimitive?.content == "sig" }
            ?.jsonObject
            ?: throw IllegalStateException("No signing key found")

        val modulus = keyContent["n"].toString().removeSurrounding("\"")
        val exponent = keyContent["e"].toString().removeSurrounding("\"")

        val spec = RSAPublicKeySpec(
            BigInteger(1, Base64.getUrlDecoder().decode(modulus)),
            BigInteger(1, Base64.getUrlDecoder().decode(exponent))
        )

        KeyFactory.getInstance("RSA").generatePublic(spec)
    } finally {
        client.close()
    }
}

class AuthenticationException(message: String) : RuntimeException(message)