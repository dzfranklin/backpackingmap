package com.backpackingmap.backpackingmap.net

import com.backpackingmap.backpackingmap.BuildConfig
import com.backpackingmap.backpackingmap.net.auth.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val AUTHORIZATION = "Authorization"
private const val BASE_URL = BuildConfig.API_BASE_URL

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface ApiService {
    @POST("registration")
    suspend fun register(@Body request: RegisterRequest):
            Response<AuthInfo, RegisterResponseError>

    @POST("session")
    suspend fun createSession(@Body request: CreateSessionRequest):
            Response<AuthInfo, CreateSessionResponseError>

    @DELETE("session")
    suspend fun deleteSession(@Header(AUTHORIZATION) auth: String)

    @POST("/session/renew")
    suspend fun renewSession(@Header(AUTHORIZATION) auth: String):
            Response<AuthInfo, RenewSessionResponseError>
}

object Api {
    val service: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}