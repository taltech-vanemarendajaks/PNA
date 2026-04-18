package com.pna.backend.plugins

import com.pna.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.application.install

fun Application.configureHttp(appConfig: AppConfig) {
    install(CORS) {
        appConfig.allowedOrigins.forEach { origin ->
            allowHost(origin.host, schemes = origin.schemes)
        }

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        allowCredentials = true
    }
}