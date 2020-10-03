package com.backpackingmap.backpackingmap.net.auth

data class CreateSessionRequest(val user: CreateSessionRequestUser)

data class CreateSessionRequestUser(
    val email: String,
    val password: String
)

