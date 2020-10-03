package com.backpackingmap.backpackingmap.net

import com.backpackingmap.backpackingmap.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

private const val BASE_URL = BuildConfig.API_BASE_URL

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface AuthApiService {
    @POST("registration")
    suspend fun register(@Body request: RegisterRequest):
            Response<AuthInfo, RegisterResponseError>

    @POST("session")
    suspend fun createSession(@Body request: CreateSessionRequest):
            Response<AuthInfo, CreateSessionResponseError>

    @DELETE("session")
    suspend fun deleteSession()

    @POST("/session/renew")
    suspend fun renewSession():
            Response<AuthInfo, RenewSessionResponseError>
}

object AuthApi {
    val service: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}