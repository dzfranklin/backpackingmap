package com.backpackingmap.backpackingmap.repo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import arrow.core.Either
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.DbUser
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.map.wmts.*
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.Api
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.net.tile.TileRequestPosition
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
            return users[0]
        } else {
            throw IllegalStateException("Exactly one user must exist")
        }
    }

    private val accessTokenCache = AccessTokenCache {
        Timber.i("Renewing access token")

        val user = getUser()

        makeUnauthenticatedRemoteRequest() { api.renewSession(user.renewalToken) }
            .mapLeft {
                Timber.w("Failed to renew access token: %s", it)
                it
            }
            .map {
                val accessToken = AccessToken(it.access_token)
                val renewalToken = RenewalToken(it.renewal_token)

                updateUserRenewalToken(renewalToken)
                accessToken
            }
    }

    init {
        accessTokenCache.prime()
    }

    private val tileCache = TileCache()

    suspend fun getTile(
        service: WmtsServiceConfig,
        layer: WmtsLayerConfig,
        set: WmtsTileMatrixSetConfig,
        matrix: WmtsTileMatrixConfig,
        position: WmtsTilePosition,
    ): Either<GetTileError, Bitmap> {
        val cached = tileCache.get(service, layer, set, matrix, position)
        if (cached != null) {
            return Either.right(cached)
        }

        val request = GetTileRequest(
            serviceIdentifier = service.identifier,
            layerIdentifier = layer.identifier,
            setIdentifier = set.identifier,
            matrixIdentifier = matrix.identifier,
            position = TileRequestPosition(row = position.row, col = position.col)
        )

        return makeRemoteRequestForBody(accessTokenCache) { token ->
            api.getTile(token, request)
        }
            .map { BitmapFactory.decodeStream(it.byteStream()) }
            .map {
                tileCache.insert(service, layer, set, matrix, position, it)
                it
            }
            .mapLeft { GetTileError.Remote(it) }
    }

    companion object {
        @Volatile
        private var INSTANCE: Repo? = null

        fun fromApplication(application: Application): Repo? {
            val prefs = BackpackingmapSharedPrefs.fromApplication(application)

            return if (prefs.isLoggedIn) {
                val tempInstance = INSTANCE
                if (tempInstance != null) {
                    return tempInstance
                }
                synchronized(this) {
                    val db = Db.getDatabase(application)
                    val instance = Repo(prefs, db.userDao())

                    INSTANCE = instance
                    return instance
                }
            } else {
                null
            }
        }
    }
}
