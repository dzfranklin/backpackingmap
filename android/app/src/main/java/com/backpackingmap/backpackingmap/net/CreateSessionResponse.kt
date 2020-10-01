package com.backpackingmap.backpackingmap.net

data class CreateSessionResponse (
    val data: AuthTokens?,
    val error: CreateSessionResponseError?
)

data class CreateSessionResponseError(val message: String)
