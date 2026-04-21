package com.example.callgrabber

import android.content.Context
import com.example.callgrabber.apis.AuthApiService
import com.example.callgrabber.apis.CallApiService
import com.example.callgrabber.auth.AuthInterceptor
import com.example.callgrabber.auth.TokenRefreshAuthenticator
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiProvider {

    private const val PREFS_NAME = "AppSettings"
    private const val KEY_SERVER_URL = "server_url"

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedService: CallApiService? = null

    @Volatile
    private var cachedAuthBaseUrl: String? = null

    @Volatile
    private var cachedAuthService: AuthApiService? = null

    fun getCallApiService(context: Context): CallApiService {
        val appContext = context.applicationContext
        val baseUrl = getServerAddress(appContext)

        val existingService = cachedService
        if (existingService != null && cachedBaseUrl == baseUrl) {
            return existingService
        }

        synchronized(this) {
            val doubleCheckService = cachedService
            if (doubleCheckService != null && cachedBaseUrl == baseUrl) {
                return doubleCheckService
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(appContext))
                .authenticator(
                    TokenRefreshAuthenticator(
                        context = appContext
                    )
                )
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(CallApiService::class.java)

            cachedBaseUrl = baseUrl
            cachedService = service

            return service
        }
    }

    fun getAuthApiService(context: Context): AuthApiService {
        val appContext = context.applicationContext
        val baseUrl = getServerAddress(appContext)

        val existingService = cachedAuthService
        if (existingService != null && cachedAuthBaseUrl == baseUrl) {
            return existingService
        }

        synchronized(this) {
            val doubleCheckService = cachedAuthService
            if (doubleCheckService != null && cachedAuthBaseUrl == baseUrl) {
                return doubleCheckService
            }

            val okHttpClient = OkHttpClient.Builder()
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(AuthApiService::class.java)

            cachedAuthBaseUrl = baseUrl
            cachedAuthService = service

            return service
        }
    }

    fun clearCache() {
        synchronized(this) {
            cachedBaseUrl = null
            cachedService = null
            cachedAuthBaseUrl = null
            cachedAuthService = null
        }
    }

    private fun getServerAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SERVER_URL, BuildConfig.DEFAULT_SERVER_URL)?.trim().orEmpty()

        val withScheme = when {
            raw.startsWith("http://", ignoreCase = true) -> raw
            raw.startsWith("https://", ignoreCase = true) -> raw
            else -> "http://$raw"
        }

        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}