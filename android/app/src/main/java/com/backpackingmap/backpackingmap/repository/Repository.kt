package com.backpackingmap.backpackingmap.repository

import com.backpackingmap.backpackingmap.net.AuthApi
import com.backpackingmap.backpackingmap.net.RegisterRequest
import com.backpackingmap.backpackingmap.net.RegisterRequestUser
import retrofit2.HttpException
import timber.log.Timber

object Repository {
    suspend fun register(email: String, password: String): RegisterError? {
        Timber.i("Attempting to register $email")

        val request = RegisterRequest(RegisterRequestUser(email, password))

        val out = try {
            val response = AuthApi.service.register(request)
             when {
                response.error != null -> {
                    RegisterError.Api(response.error)
                }
                response.data != null -> {
                    null
                }
                else -> {
                    RegisterError.Server("Neither error nor data", IllegalStateException())
                }
            }
        } catch (throwable: HttpException) {
            RegisterError.Server("Status code: ${throwable.code()}", throwable)
        } catch (throwable: Throwable) {
            RegisterError.Network(throwable)
        }

        if (out != null) {
            Timber.w(out)
        } else {
            Timber.i("Successfully registered")
        }

        return out
    }
}