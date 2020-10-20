package com.backpackingmap.backpackingmap.repo

import android.app.ActivityManager
import android.content.Context
import com.backpackingmap.backpackingmap.db.Db
import com.backpackingmap.backpackingmap.db.user.DbUser
import com.backpackingmap.backpackingmap.db.user.UserDao
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.Api
import com.backpackingmap.backpackingmap.net.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class Repo(
    override val coroutineContext: CoroutineContext,
    private val prefs: BackpackingmapSharedPrefs,
    private val userDao: UserDao,
    private val api: ApiService,
    private val memoryClass: Int,
) : CoroutineScope {
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

    private val tileRepoSize = ((memoryClass.toDouble() * MB_TO_BYTES) * 0.75).toInt()
    val tileRepo = TileRepo(coroutineContext, accessTokenCache, api, tileRepoSize)


    companion object {
        private const val MB_TO_BYTES = 1e6

        @Volatile
        private var INSTANCE: Repo? = null

        fun fromContext(context: Context): Repo? {
            val prefs = BackpackingmapSharedPrefs.fromContext(context)

            return if (prefs.isLoggedIn) {
                val tempInstance = INSTANCE
                if (tempInstance != null) {
                    return tempInstance
                }
                synchronized(this) {
                    val db = Db.getDatabase(context)
                    val scope = CoroutineScope(Dispatchers.Default)
                    val api = Api.fromContext(context)

                    val activityManager =
                        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val memoryClass = activityManager.memoryClass

                    val instance =
                        Repo(scope.coroutineContext, prefs, db.userDao(), api, memoryClass)

                    INSTANCE = instance
                    return instance
                }
            } else {
                null
            }
        }
    }
}
