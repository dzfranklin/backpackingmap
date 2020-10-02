package com.backpackingmap.backpackingmap.net

data class RegisterResponse(
    val data: AuthTokens?,
    val error: RegisterResponseError?
)

data class RegisterResponseError(
    val message: String,
    val field_errors: RegisterResponseFieldErrors
)

data class RegisterResponseFieldErrors(
    val email: String?,
    val password: String?
)
