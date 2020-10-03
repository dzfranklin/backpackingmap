package com.backpackingmap.backpackingmap.repo

import com.backpackingmap.backpackingmap.net.AuthApi
import com.backpackingmap.backpackingmap.net.RegisterRequest
import com.backpackingmap.backpackingmap.net.RegisterRequestUser
import com.backpackingmap.backpackingmap.net.RegisterResponseError
import retrofit2.HttpException
import timber.log.Timber

object Repo {
    suspend fun register(email: String, password: String): RemoteError<RegisterResponseError>? {
        Timber.i("Attempting to register $email")

        val request = RegisterRequest(RegisterRequestUser(email, password))

        val out = try {
            val response = AuthApi.service.register(request)
             when {
                response.error != null -> {
                    RemoteError.Api(response.error)
                }
                response.data != null -> {
                    null
                }
                else -> {
                    RemoteError.Server("Neither error nor data", IllegalStateException())
                }
            }
        } catch (throwable: HttpException) {
            RemoteError.Server("Status code: ${throwable.code()}", throwable)
        } catch (throwable: Throwable) {
            RemoteError.Network(throwable)
        }

        if (out != null) {
            Timber.w("Failed to register: %s", out)
        } else {
            Timber.i("Successfully registered")
        }

        return out
    }
}