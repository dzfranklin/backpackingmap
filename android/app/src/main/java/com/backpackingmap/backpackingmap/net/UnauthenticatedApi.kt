package com.backpackingmap.backpackingmap.net

import com.backpackingmap.backpackingmap.BuildConfig
import com.backpackingmap.backpackingmap.net.auth.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private const val BASE_URL = BuildConfig.API_BASE_URL

interface UnauthenticatedApiService {
    @POST("registration")
    suspend fun register(@Body request: RegisterRequest):
            Response<RegisterResponseError, AuthInfo>

    @POST("session")
    suspend fun createSession(@Body request: CreateSessionRequest):
            Response<CreateSessionResponseError, AuthInfo>
}

object UnauthenticatedApi {
    val service: UnauthenticatedApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(BASE_URL)
            .build()

        retrofit.create(UnauthenticatedApiService::class.java)
    }
}