package com.example.callgrabber

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SearchNumberRequest(
    val number: String
)

data class SearchNumberResponse(
    val message: String
)

interface CallApiService {
    @POST("api/v1/number/search")
    suspend fun searchNumber(
        @Body request: SearchNumberRequest
    ): Response<SearchNumberResponse>
}