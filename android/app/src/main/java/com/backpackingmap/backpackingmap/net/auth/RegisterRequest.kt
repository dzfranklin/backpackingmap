package com.backpackingmap.backpackingmap.net.auth

data class RegisterRequest(val user: RegisterRequestUser)

data class RegisterRequestUser(
    val email: String,
    val password: String
)
