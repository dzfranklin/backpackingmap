package com.backpackingmap.backpackingmap.net.auth

import com.backpackingmap.backpackingmap.net.ResponseErrorWithMessage

data class CreateSessionResponseError(override val message: String): ResponseErrorWithMessage
