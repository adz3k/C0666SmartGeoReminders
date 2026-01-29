package com.example.smartgeoreminders

import java.security.MessageDigest

class AuthRepository(private val userDao: UserDao) {

    suspend fun register(name: String, email: String, password: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        val cleanPassword = password.trim()

        if (cleanName.isBlank()) return Result.failure(Exception("Name is required"))
        if (cleanEmail.isBlank()) return Result.failure(Exception("Email is required"))
        if (cleanPassword.length < 6) return Result.failure(Exception("Password must be at least 6 characters"))

        val existing = userDao.findByEmail(cleanEmail)
        if (existing != null) return Result.failure(Exception("Email already registered"))

        val hash = sha256(cleanPassword)
        userDao.insert(UserEntity(name = cleanName, email = cleanEmail, passwordHash = hash))
        return Result.success(Unit)
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()

        val user = userDao.findByEmail(cleanEmail) ?: return Result.failure(Exception("No account found"))
        val hash = sha256(cleanPassword)

        return if (user.passwordHash == hash) Result.success(user)
        else Result.failure(Exception("Invalid password"))
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
