package com.backpackingmap.backpackingmap.net

import com.backpackingmap.backpackingmap.BuildConfig
import com.backpackingmap.backpackingmap.net.auth.AuthInfo
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import com.backpackingmap.backpackingmap.net.tile.TileType
import com.backpackingmap.backpackingmap.repo.RenewalToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = BuildConfig.API_BASE_URL

interface ApiService {
    @POST("session/renew")
    suspend fun renewSession(@Header(AUTH) token: RenewalToken):
            Response<AuthInfo, RenewSessionResponseError>

    @DELETE("session")
    suspend fun deleteSession(@Header(AUTH) token: AccessToken)

    @GET("tile/{type}")
    suspend fun getTile(
        @Header(AUTH) token: AccessToken,
        @Path("type") type: TileType,
        @Query("row") row: Int,
        @Query("col") col: Int,
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