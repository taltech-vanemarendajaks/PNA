package com.example.callgrabber.apis

import com.example.callgrabber.RefreshTokenRequest
import com.example.callgrabber.RefreshTokenResponse
import com.example.callgrabber.SearchNumberRequest
import com.example.callgrabber.SearchNumberResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CallApiService {
    @POST("api/v1/number/android-search")
    suspend fun searchNumber(
        @Body request: SearchNumberRequest
    ): Response<SearchNumberResponse>

    @POST("api/v1/number/android-refresh")
    suspend fun refreshTokenBlocking(
        @Body request: RefreshTokenRequest
    ): Response<RefreshTokenResponse>
}