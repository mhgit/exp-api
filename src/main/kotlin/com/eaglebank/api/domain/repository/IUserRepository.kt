package com.eaglebank.api.domain.repository

import com.eaglebank.api.domain.model.User

interface IUserRepository {
    fun createUser(user: User): User
    fun getUserById(id: String): User?
    fun updateUser(id: String, user: User): User?
    fun deleteUser(id: String): Boolean
    fun getAllUsers(): List<User>
}