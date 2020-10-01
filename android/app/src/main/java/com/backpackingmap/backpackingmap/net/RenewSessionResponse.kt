package com.backpackingmap.backpackingmap.net

data class RenewSessionResponse(
    val data: AuthTokens?,
    val error: RenewSessionResponseError?
)

data class RenewSessionResponseError(val message: String)
