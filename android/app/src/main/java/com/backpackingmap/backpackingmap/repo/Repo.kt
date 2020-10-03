package com.backpackingmap.backpackingmap.repo

import android.app.Application
import android.content.SharedPreferences
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.User
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.net.*
import retrofit2.HttpException
import timber.log.Timber

class Repo(private val sharedPreferences: SharedPreferences, private val userDao: UserDao) {
    val isLoggedIn
        get() = sharedPreferences.getBoolean(IS_LOGGED_IN, false)

    suspend fun getUser(): User? {
        val users = userDao.getUsers()
        return when (users.size) {
            0 -> null
            1 -> users[0]
            else -> throw IllegalStateException("More than one user exists in DB")
        }
    }

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

    private suspend fun setLoggedInUser(info: AuthInfo) {
        val prefEditor = sharedPreferences.edit()

        val user = User(info.user_id, info.access_token, info.renewal_token)
        userDao.insertUser(user)

        prefEditor.putBoolean(IS_LOGGED_IN, true)
        if (!prefEditor.commit()) {
            userDao.deleteUsers() // Undo the earlier insert to go back to a consistent state
            throw IllegalStateException("Failed to write is_logged_in=true to shared preferences")
        }
    }

    companion object {
        const val IS_LOGGED_IN = "is_logged_in"

        fun fromApplication(application: Application): Repo {
            val db = Db.getDatabase(application)

            // Mode 0 is application-private. Accessing the constant requires a Context
            // <https://developer.android.com/reference/android/content/Context#MODE_PRIVATE>
            val prefs = application.getSharedPreferences("backpackingmap_prefs", 0)

            return Repo(prefs, db.userDao())
        }
    }
}