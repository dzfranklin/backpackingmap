package com.backpackingmap.backpackingmap.repo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import arrow.core.Either
import arrow.core.flatMap
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.DbUser
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.map.wmts.*
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.Api
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.net.tile.TileRequestPosition

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
        service: WmtsServiceConfig,
        layer: WmtsLayerConfig,
        set: WmtsTileMatrixSetConfig,
        matrix: WmtsTileMatrixConfig,
        position: WmtsTilePosition,
    ): Either<GetTileError, Bitmap> {
        // TODO cache
        val request = GetTileRequest(
            serviceIdentifier = service.identifier,
            layerIdentifier = layer.identifier,
            setIdentifier = set.identifier,
            matrixIdentifier = matrix.identifier,
            position = TileRequestPosition(row = position.row, col = position.col)
        )

        return getAccessToken()
            .flatMap { accessToken ->
                makeRemoteRequestForBody { api.getTile(accessToken, request) }
            }
            .map { BitmapFactory.decodeStream(it.byteStream()) }
            .mapLeft(::getTileErrorFromRemoteError)
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