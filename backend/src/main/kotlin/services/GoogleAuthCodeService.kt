package com.pna.backend.services

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

class GoogleAuthCodeService(
    private val clientId: String,
    private val clientSecret: String
) {
    fun exchangeCodeForIdToken(code: String, redirectUri: String): String? {
        return runCatching {
            GoogleAuthorizationCodeTokenRequest(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                code,
                redirectUri
            ).execute().idToken
        }.getOrNull()
    }
}
