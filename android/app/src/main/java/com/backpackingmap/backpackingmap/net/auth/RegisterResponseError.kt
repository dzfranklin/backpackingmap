package com.backpackingmap.backpackingmap.net.auth

data class RegisterResponseError(
    val message: String,
    val field_errors: RegisterResponseFieldErrors
)