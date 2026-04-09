package com.example.callgrabber

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiProvider {

    private const val PREFS_NAME = "AppSettings"
    private const val KEY_SERVER_IP = "server_ip"

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedService: CallApiService? = null

    fun getCallApiService(context: Context): CallApiService {
        val baseUrl = getServerAddress(context)

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
                .addInterceptor(AuthInterceptor(context.applicationContext))
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

    fun clearCache() {
        synchronized(this) {
            cachedBaseUrl = null
            cachedService = null
        }
    }

    private fun getServerAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SERVER_IP, BuildConfig.DEFAULT_SERVER_URL)?.trim().orEmpty()

        val withScheme = when {
            raw.startsWith("http://", ignoreCase = true) -> raw
            raw.startsWith("https://", ignoreCase = true) -> raw
            else -> "http://$raw"
        }

        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}