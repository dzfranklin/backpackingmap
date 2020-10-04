package com.backpackingmap.backpackingmap.net.auth

import com.backpackingmap.backpackingmap.net.ResponseErrorWithMessage

data class RegisterResponseError(
    override val message: String,
    val field_errors: RegisterResponseFieldErrors
): ResponseErrorWithMessage