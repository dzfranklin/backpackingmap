package com.backpackingmap.backpackingmap.repo

import arrow.core.Either
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.Response
import com.backpackingmap.backpackingmap.net.auth.AuthInfo
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import kotlin.reflect.KClass

class MakeRemoteRequestKtTest {
    private val success = AuthInfo(1, "ACCESS_TOKEN", "RENEWAL_TOKEN")
    private val error = RenewSessionResponseError("ERROR_MESSAGE")
    private val errorResponseBody = ResponseBody.create(MediaType.get("text/plain"), "ERROR_BODY")
    private lateinit var accessTokenCache: AccessTokenCache

    @Before
    fun setup() {
        accessTokenCache = AccessTokenCache { Either.right(AccessToken("ACCESS_TOKEN")) }
    }

    @Test
    fun `makeUnauthenticatedRemoteRequest success`() {
        val response = runBlocking {
            makeUnauthenticatedRemoteRequest { Response(null, success) }
        }
        assertThat(response, `is`(Either.right(success)))
    }

    @Test
    fun `makeUnauthenticatedRemoteRequest treats error and success as error`() {
        val response = runBlocking {
            makeUnauthenticatedRemoteRequest { Response(error, success) }
        }
        assertThat(response, `is`(Either.left(UnauthenticatedRemoteError.Api(error))))
    }

    @Test
    fun `makeUnauthenticatedRemoteRequest empty response treated as server error`() {
        val response = runBlocking {
            makeUnauthenticatedRemoteRequest { Response(null, null) }
        }

        assertIsLeftContaining(response, UnauthenticatedRemoteError.Server::class)
    }

    @Test
    fun `makeUnauthenticatedRemoteRequest server error`() {
        val response = runBlocking {
            makeUnauthenticatedRemoteRequest<Nothing, Nothing> {
                throw HttpException(retrofit2.Response.error<ResponseBody>(500, errorResponseBody))
            }
        }

        assertIsLeftContaining(response, UnauthenticatedRemoteError.Server::class)
    }

    @Test
    fun `makeUnauthenticatedRemoteRequest any throwable is treated as network error`() {
        val response = runBlocking {
            makeUnauthenticatedRemoteRequest<Nothing, Nothing> {
                throw Throwable("Some random error")
            }
        }

        assertIsLeftContaining(response, UnauthenticatedRemoteError.Network::class)
    }

    @Test
    fun `makeUnauthenticatedRemoteRequest api error`() {
        val response = runBlocking {
            makeUnauthenticatedRemoteRequest {
                Response(error, null)
            }
        }

        assertThat(response, `is`(Either.left(UnauthenticatedRemoteError.Api(error))))
    }

    @Test
    fun `makeRemoteRequestForBody treats http code 401 as RemoteError$Auth`() {
        val response = runBlocking {
            makeRemoteRequestForBody(accessTokenCache) {
                throw HttpException(retrofit2.Response.error<ResponseBody>(401, errorResponseBody))
            }
        }

        assertIsLeftContaining(response, RemoteError.Auth::class)
    }

    private fun <A, B, C : Any> assertIsLeftContaining(response: Either<A, B>, klass: KClass<C>) {
        assertThat(response, instanceOf(Either.Left::class.java))
        val contents = (response as Either.Left).a
        assertThat(contents, instanceOf(klass.java))
    }
}