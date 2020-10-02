package com.backpackingmap.backpackingmap.repository

import com.backpackingmap.backpackingmap.net.RegisterResponseError

sealed class RegisterError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Throwable(message, cause) {

    data class Network(override val cause: Throwable) :
        RegisterError()

    data class Server(val type: String, val t: Throwable) :
        RegisterError(message = type, cause = t)

    data class Api(val response: RegisterResponseError) :
        RegisterError(message = response.toString())
}