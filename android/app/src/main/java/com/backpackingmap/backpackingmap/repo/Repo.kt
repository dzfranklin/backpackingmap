package com.backpackingmap.backpackingmap.repo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.User
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.net.Api
import com.backpackingmap.backpackingmap.net.ApiService
import com.backpackingmap.backpackingmap.net.RenewalToken
import com.backpackingmap.backpackingmap.net.tile.TileType
import timber.log.Timber

class Repo(private val prefs: BackpackingmapSharedPrefs, private val userDao: UserDao) {
    init {
        if (!prefs.isLoggedIn) {
            throw IllegalStateException("Cannot create Repo when not logged in")
        }
    }

    private suspend fun getUser(): User {
        val users = userDao.getUsers()
        if (users.size == 1) {
            return users[0]
        } else {
            throw IllegalStateException("Exactly one user must exist")
        }
    }

    private val apiServiceCache: ApiService? = null

    private suspend fun getApi(): ApiService =
        apiServiceCache
            ?: getUser().let { user ->
                val tokens = RenewalToken(user.renewal_token)
                Api.createService(tokens)
            }

    suspend fun getTile(type: TileType, row: Int, col: Int): Bitmap {
        // TODO cache
        Timber.i("Attempting to get tile with, %s row: %d col: %d", type, row, col)

        val response = getApi().getTile(type, row, col)
        return BitmapFactory.decodeStream(response.byteStream())
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