package com.backpackingmap.backpackingmap.net

data class RegisterResponseError(
    val message: String,
    val field_errors: RegisterResponseFieldErrors
)