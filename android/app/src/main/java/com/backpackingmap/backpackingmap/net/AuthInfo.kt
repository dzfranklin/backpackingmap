package com.backpackingmap.backpackingmap.net

data class AuthInfo(
    val user_id: Int,
    val access_token: String,
    val renewal_token: String
)
