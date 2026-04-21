package com.example.callgrabber.auth

import android.content.Context
import android.util.Log
import com.example.callgrabber.ApiProvider
import com.example.callgrabber.RefreshTokenRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenRefreshAuthenticator(
    private val context: Context
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            Log.e("CALL_GRABBER", "Token refresh loop detected. Clearing auth.")
            AuthStorage.clear(context)
            return null
        }

        val refreshToken = AuthStorage.getRefreshToken(context)

        if (refreshToken.isNullOrBlank()) {
            Log.e("CALL_GRABBER", "No refresh token found.")
            AuthStorage.clear(context)
            return null
        }

        synchronized(this) {
            val currentAccessToken = AuthStorage.getToken(context)

            val requestAccessToken = response.request()
                .header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestAccessToken) {
                return response.request()
                    .newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshResponse = try {
                ApiProvider.getAuthApiService(context)
                    .refreshTokenBlocking(
                        RefreshTokenRequest(refreshToken = refreshToken)
                    )
                    .execute()
            } catch (e: Exception) {
                Log.e("CALL_GRABBER", "Refresh token request failed", e)
                null
            }

            if (refreshResponse == null || !refreshResponse.isSuccessful) {
                Log.e(
                    "CALL_GRABBER",
                    "Refresh failed: ${refreshResponse?.code()} ${refreshResponse?.errorBody()?.string()}"
                )

                AuthStorage.clear(context)
                return null
            }

            val body = refreshResponse.body()

            if (body == null || body.token.isBlank() || body.refreshToken.isBlank()) {
                Log.e("CALL_GRABBER", "Refresh response had empty tokens.")
                AuthStorage.clear(context)
                return null
            }

            AuthStorage.saveTokens(
                context = context,
                accessToken = body.token,
                refreshToken = body.refreshToken
            )

            Log.d("CALL_GRABBER", "Access token refreshed successfully.")

            return response.request()
                .newBuilder()
                .header("Authorization", "Bearer ${body.token}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var current = response.priorResponse()

        while (current != null) {
            count++
            current = current.priorResponse()
        }

        return count
    }
}