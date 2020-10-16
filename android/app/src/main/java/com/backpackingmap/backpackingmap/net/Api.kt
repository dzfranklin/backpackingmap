package com.backpackingmap.backpackingmap.net

import com.backpackingmap.backpackingmap.BuildConfig
import com.backpackingmap.backpackingmap.net.auth.AuthInfo
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.repo.RenewalToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST

private const val BASE_URL = BuildConfig.API_BASE_URL

interface ApiService {
    @POST("session/renew")
    suspend fun renewSession(@Header(AUTH) token: RenewalToken):
            Response<AuthInfo, RenewSessionResponseError>

    @DELETE("session")
    suspend fun deleteSession(@Header(AUTH) token: AccessToken)

    @POST("tile")
    suspend fun getTile(
        @Header(AUTH) token: AccessToken,
        @Body request: GetTileRequest
    ): ResponseBody

    companion object {
        private const val AUTH = "Authorization"
    }
}

object Api {
    val service: ApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(BASE_URL)
            .build()

        retrofit.create(ApiService::class.java)
    }
}