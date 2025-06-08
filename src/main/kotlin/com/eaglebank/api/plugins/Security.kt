package com.eaglebank.api.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

@Serializable
data class KeycloakPublicKeyResponse(
    val keys: List<KeycloakKey>
)

@Serializable
data class KeycloakKey(
    val kid: String,
    val kty: String,
    val n: String,
    val e: String,
    val use: String,
    val alg: String
)

class KeycloakRsaKeyProvider(private val issuer: String) {
    private var publicKey: RSAPublicKey? = null

    suspend fun getKey(): RSAPublicKey {
        if (publicKey == null) {
            publicKey = fetchPublicKey()
        }
        return publicKey!!
    }

    private suspend fun fetchPublicKey(): RSAPublicKey {
        val client = HttpClient(CIO)
        val response = client.get("$issuer/protocol/openid-connect/certs")
        val keysResponse = Json.decodeFromString<KeycloakPublicKeyResponse>(response.body())
        client.close()

        val key = keysResponse.keys.first { it.use == "sig" }
        val modulus = Base64.getUrlDecoder().decode(key.n)
        val exponent = Base64.getUrlDecoder().decode(key.e)

        val spec = X509EncodedKeySpec(modulus)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(spec)
        return publicKey as RSAPublicKey
    }
}

fun Application.configureSecurity() {
    val config = environment.config
    val keycloakRealm = config.property("keycloak.realm").getString()
    val keycloakUrl = config.property("keycloak.serverUrl").getString()
    val keycloakIssuer = "$keycloakUrl/realms/$keycloakRealm"

    val keyProvider = KeycloakRsaKeyProvider(keycloakIssuer)
    val publicKey = runBlocking { keyProvider.getKey() }

    authentication {
        jwt("auth-jwt") {
            verifier(
                JWT
                    .require(Algorithm.RSA256(publicKey, null))
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
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Token is not valid or has expired"))
            }
        }
    }
}