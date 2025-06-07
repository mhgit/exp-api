package com.eaglebank.api.infra.security

import org.mindrot.jbcrypt.BCrypt

object PasswordService {
    /**
     * Hash a password using BCrypt
     * @param plainTextPassword The password to hash
     * @return The hashed password
     */
    fun hashPassword(plainTextPassword: String): String {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12))
    }

    /**
     * Verify a password against a hash
     * @param plainTextPassword The password to check
     * @param hashedPassword The hash to check against
     * @return true if the password matches the hash
     */
    fun verifyPassword(plainTextPassword: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(plainTextPassword, hashedPassword)
    }
}