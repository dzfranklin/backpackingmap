package com.backpackingmap.backpackingmap.repo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import arrow.core.Either
import arrow.core.flatMap
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.DbUser
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.Api
import com.backpackingmap.backpackingmap.net.ResponseErrorWithMessage
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import com.backpackingmap.backpackingmap.net.tile.TileType
import timber.log.Timber

class Repo(private val prefs: BackpackingmapSharedPrefs, private val userDao: UserDao) {
    val api = Api.service

    init {
        if (!prefs.isLoggedIn) {
            throw IllegalStateException("Cannot create Repo when not logged in")
        }
    }

    private suspend fun getUser(): User {
        val dbUser = getDbUser()
        return User(dbUser.id, RenewalToken(dbUser.renewalToken))
    }


    private suspend fun updateUserRenewalToken(token: RenewalToken) {
        val dbUser = getDbUser()
        dbUser.renewalToken = token.toString()
        userDao.updateUsers(dbUser)
    }


    private suspend fun getDbUser(): DbUser {
        val users = userDao.getUsers()
        if (users.size == 1) {
            val dbUser = users[0]
            return dbUser
        } else {
            throw IllegalStateException("Exactly one user must exist")
        }
    }

    private var accessTokenCache: AccessToken? = null
    private suspend fun getAccessToken(): Either<RemoteError<RenewSessionResponseError>, AccessToken> {
        val cached = accessTokenCache
        return if (cached != null) {
            Either.right(cached)
        } else {
            renewAccessToken()
        }
    }

    private suspend fun renewAccessToken(): Either<RemoteError<RenewSessionResponseError>, AccessToken> {
        val user = getUser()

        return makeRemoteRequest { api.renewSession(user.renewalToken) }
            .map {
                val accessToken = AccessToken(it.access_token)
                val renewalToken = RenewalToken(it.renewal_token)

                updateUserRenewalToken(renewalToken)
                accessTokenCache = accessToken
                accessToken
            }
    }

    suspend fun getTile(
        type: TileType,
        row: Int,
        col: Int,
    ): Either<RemoteError<ResponseErrorWithMessage>, Bitmap> {
        // TODO cache
        Timber.i("Attempting to get tile with, %s row: %d col: %d", type, row, col)

        return getAccessToken()
            .flatMap { accessToken ->
                makeRemoteRequestForBody { api.getTile(accessToken, type, row, col) }
            }
            .map { BitmapFactory.decodeStream(it.byteStream()) }
    }

    companion object {
        fun fromApplication(application: Application): Repo? {
            val db = Db.getDatabase(application)
            val prefs = BackpackingmapSharedPrefs(application)

            return if (prefs.isLoggedIn) {
                Repo(prefs, db.userDao())
            } else {
                null
            }
        }
    }
}