// Import required libraries
import java.util.Base64          // For encoding the token in base64 format
import java.security.SecureRandom // For generating cryptographically secure random numbers
import java.io.File              // For file operations

// Create a secure random number generator
val random = SecureRandom()
// Create a byte array of size 32 (256 bits - considered cryptographically secure)
val tokenBytes = ByteArray(32)
// Fill the byte array with random bytes
random.nextBytes(tokenBytes)
// Convert the random bytes to a Base64 string for easier handling
val token = Base64.getEncoder().encodeToString(tokenBytes)