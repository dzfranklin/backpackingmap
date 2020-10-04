package com.backpackingmap.backpackingmap.net

import arrow.syntax.function.memoize
import com.backpackingmap.backpackingmap.BuildConfig
import com.backpackingmap.backpackingmap.net.auth.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private const val AUTHORIZATION = "Authorization"
private const val BASE_URL = BuildConfig.API_BASE_URL

interface UnauthenticatedApiService {
    @POST("registration")
    suspend fun register(@Body request: RegisterRequest):
            Response<AuthInfo, RegisterResponseError>

    @POST("session")
    suspend fun createSession(@Body request: CreateSessionRequest):
            Response<AuthInfo, CreateSessionResponseError>
}

object UnauthenticatedApi {
    val createService = ::createServiceUnmemoized.memoize()

    private fun createServiceUnmemoized(): UnauthenticatedApiService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(BASE_URL)
            .build()

        return retrofit.create(UnauthenticatedApiService::class.java)
    }
}