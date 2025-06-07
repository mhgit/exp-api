package com.eaglebank.api.infra.persistence

import com.eaglebank.api.domain.model.Address
import com.eaglebank.api.domain.model.User
import com.eaglebank.api.infra.keycloak.KeycloakService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.*


class UserRepositoryTest {
    private lateinit var keycloakService: KeycloakService
    private lateinit var userRepository: UserRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testAddress = Address(
        line1 = "123 Test St",
        line2 = null,
        line3 = null,
        town = "Testville",
        county = "Testshire",
        postcode = "TE1 1ST"
    )

    private val testUser = User(
        id = "usr-test123456",
        name = "Test User",
        address = testAddress,
        phoneNumber = "07700900000",
        email = "test@example.com",
        keycloakId = "kc-123456",
        createdTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
        updatedTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Set up in-memory H2 database
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        // Create tables
        transaction {
            SchemaUtils.create(UserTable)
        }

        keycloakService = mockk()
        userRepository = UserRepository(keycloakService)

        // Setup basic Keycloak mock responses
        coEvery {
            keycloakService.createUser(any(), any(), any(), any())
        } returns "kc-123456"
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()

        transaction {
            SchemaUtils.drop(UserTable)
        }
    }

    @Test
    fun `createUser should successfully create user`() = runTest {
        // Given
        val userToCreate = testUser.copy(
            id = User.generateId(),  // Generate a valid ID
            keycloakId = null
        )

        // When
        val createdUser = userRepository.createUser(userToCreate)

        // Then
        assertNotNull(createdUser)
        assertTrue(createdUser.id.startsWith("usr-"))
        assertEquals(testUser.email, createdUser.email)
        assertEquals(testUser.name, createdUser.name)
        assertNotNull(createdUser.keycloakId)
    }

    @Test
    fun `getUserById should return user when exists`() = runBlocking {
        // Given
        // First create a user
        val createdUser = userRepository.createUser(testUser.copy(id = "", keycloakId = null))

        // When
        val retrievedUser = userRepository.getUserById(createdUser.id)

        // Then
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser?.id)
        assertEquals(createdUser.email, retrievedUser?.email)
    }

    @Test
    fun `getUserById should return null when user doesn't exist`() = runBlocking {
        // When
        val retrievedUser = userRepository.getUserById("usr-nonexistent")

        // Then
        assertNull(retrievedUser)
    }

    @Test
    fun `updateUser should successfully update existing user`() = runBlocking {
        // Given
        val createdUser = userRepository.createUser(testUser.copy(id = "", keycloakId = null))
        val updatedName = "Updated Test User"
        val updatedEmail = "updated@example.com"

        val userToUpdate = createdUser.copy(
            name = updatedName,
            email = updatedEmail
        )

        // Setup Keycloak mock for update
        coEvery {
            keycloakService.updateUser(any(), any(), any(), any())
        } returns Unit

        // When
        val updatedUser = userRepository.updateUser(createdUser.id, userToUpdate)

        // Then
        assertNotNull(updatedUser)
        assertEquals(updatedName, updatedUser?.name)
        assertEquals(updatedEmail, updatedUser?.email)
    }

    @Test
    fun `updateUser should return null for non-existent user`() = runBlocking {
        // When
        val updatedUser = userRepository.updateUser("usr-nonexistent", testUser)

        // Then
        assertNull(updatedUser)
    }

    @Test
    fun `deleteUser should return true when user exists`() = runBlocking {
        // Given
        val createdUser = userRepository.createUser(testUser.copy(id = "", keycloakId = null))

        // Setup Keycloak mock for delete
        coEvery {
            keycloakService.deleteUser(any())
        } returns Unit

        // When
        val result = userRepository.deleteUser(createdUser.id)

        // Then
        assertTrue(result)
        assertNull(userRepository.getUserById(createdUser.id))
    }

    @Test
    fun `deleteUser should return false when user doesn't exist`() = runBlocking {
        // When
        val result = userRepository.deleteUser("usr-nonexistent")

        // Then
        assertFalse(result)
    }

    @Test
    fun `getAllUsers should return all users`() = runBlocking {
        // Given
        val user1 = userRepository.createUser(testUser.copy(id = "", keycloakId = null))
        val user2 = userRepository.createUser(
            testUser.copy(
                id = "",
                keycloakId = null,
                email = "test2@example.com"
            )
        )

        // When
        val allUsers = userRepository.getAllUsers()

        // Then
        assertEquals(2, allUsers.size)
        assertTrue(allUsers.any { it.id == user1.id })
        assertTrue(allUsers.any { it.id == user2.id })
    }
}