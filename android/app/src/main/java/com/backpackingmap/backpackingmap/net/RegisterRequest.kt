package com.backpackingmap.backpackingmap.net

data class RegisterRequest(val user: RegisterRequestUser)

data class RegisterRequestUser(
    val email: String,
    val password: String
)
