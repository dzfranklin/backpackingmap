package com.backpackingmap.backpackingmap.repo

import android.app.Application
import arrow.core.Either
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.DbUser
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.net.UnauthenticatedApi
import com.backpackingmap.backpackingmap.net.auth.*
import timber.log.Timber

class UnauthenticatedRepo(
    private val prefs: BackpackingmapSharedPrefs,
    private val userDao: UserDao,
) {
    val api = UnauthenticatedApi.service

    suspend fun register(email: String, password: String): UnauthenticatedRemoteError<RegisterResponseError>? {
        Timber.i("Attempting to register %s", email)

        val request = RegisterRequest(RegisterRequestUser(email, password))

        return when (val result = makeUnauthenticatedRemoteRequest {
            api.register(request)
        }) {
            is Either.Left -> {
                Timber.w("Failed to register email $email with error: ${result.a}")
                result.a
            }
            is Either.Right -> {
                setLoggedInUser(result.b)
                Timber.i("Registered as user ${result.b.user_id} with email $email")
                null
            }
        }
    }

    suspend fun login(email: String, password: String): UnauthenticatedRemoteError<CreateSessionResponseError>? {
        Timber.i("Attempting to login %s", email)

        val request = CreateSessionRequest(CreateSessionRequestUser(email, password))

        return when (val result = makeUnauthenticatedRemoteRequest {
            api.createSession(request)
        }) {
            is Either.Left -> {
                Timber.w("Failed to login email $email with error: ${result.a}")
                result.a
            }
            is Either.Right -> {
                setLoggedInUser(result.b)
                Timber.i("Logged in user ${result.b.user_id} with email $email")
                null
            }
        }
    }

    private suspend fun setLoggedInUser(info: AuthInfo) {
        // NOTE: We open the edit first so that if that fails we won't bother writing to the db
        val prefEditor = prefs.edit()

        val user = DbUser(info.user_id, info.renewal_token)
        userDao.insertUser(user)

        prefs.setIsLoggedIn(prefEditor, true)
        if (!prefEditor.commit()) {
            userDao.deleteUsers() // Undo the earlier insert to go back to a consistent state
            throw IllegalStateException("Failed to write is_logged_in=true to shared preferences")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UnauthenticatedRepo? = null

        fun fromApplication(application: Application): UnauthenticatedRepo {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val db = Db.getDatabase(application)
                val prefs = BackpackingmapSharedPrefs.fromContext(application)
                val instance = UnauthenticatedRepo(prefs, db.userDao())

                INSTANCE = instance
                return instance
            }
        }
    }
}
