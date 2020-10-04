package com.backpackingmap.backpackingmap.repo

import android.app.Application
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.User
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.net.UnauthenticatedApi
import com.backpackingmap.backpackingmap.net.auth.*
import retrofit2.HttpException
import timber.log.Timber

class UnauthenticatedRepo(
    private val prefs: BackpackingmapSharedPrefs,
    private val userDao: UserDao,
) {
    val api = UnauthenticatedApi.createService()

    suspend fun register(email: String, password: String): RemoteError<RegisterResponseError>? {
        Timber.i("Attempting to register %s", email)

        val request = RegisterRequest(RegisterRequestUser(email, password))

        val out = try {
            val response = api.register(request)
            when {
                response.error != null -> {
                    RemoteError.Api(response.error)
                }
                response.data != null -> {
                    setLoggedInUser(response.data)
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

    suspend fun login(email: String, password: String): RemoteError<CreateSessionResponseError>? {
        Timber.i("Attempting to login %s", email)

        val request = CreateSessionRequest(CreateSessionRequestUser(email, password))

        val out = try {
            val response = api.createSession(request)
            when {
                response.error != null -> {
                    RemoteError.Api(response.error)
                }
                response.data != null -> {
                    setLoggedInUser(response.data)
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
            Timber.w("Failed to login in: %s", out)
        } else {
            Timber.i("Successfully logged in")
        }

        return out
    }

    private suspend fun setLoggedInUser(info: AuthInfo) {
        // NOTE: We open the edit first so that if that fails we won't bother writing to the db
        val prefEditor = prefs.edit()

        val user = User(info.user_id, info.renewal_token)
        userDao.insertUser(user)

        prefs.setIsLoggedIn(prefEditor, true)
        if (!prefEditor.commit()) {
            userDao.deleteUsers() // Undo the earlier insert to go back to a consistent state
            throw IllegalStateException("Failed to write is_logged_in=true to shared preferences")
        }
    }

    companion object {
        fun fromApplication(application: Application): UnauthenticatedRepo {
            val db = Db.getDatabase(application)
            val prefs = BackpackingmapSharedPrefs(application)

            return UnauthenticatedRepo(prefs, db.userDao())
        }
    }
}