package com.example.callgrabber.apis

import com.example.callgrabber.AuthResponse
import com.example.callgrabber.GoogleLoginRequest
import com.example.callgrabber.RefreshTokenRequest
import com.example.callgrabber.RefreshTokenResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/v1/auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/android-refresh")
    fun refreshTokenBlocking(
        @Body request: RefreshTokenRequest
    ): Call<RefreshTokenResponse>
}