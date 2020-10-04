package com.backpackingmap.backpackingmap.net.auth

import com.backpackingmap.backpackingmap.net.ResponseErrorWithMessage

data class RenewSessionResponseError(override val message: String): ResponseErrorWithMessage
