package com.pna.backend

import auth.googleAuthRoutes
import com.pna.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json

fun main() {
    val appConfig = AppConfig.load()

    embeddedServer(Netty, port = appConfig.port, host = appConfig.host) {
        module(appConfig)
    }
        .start(wait = true)
}

fun Application.module(appConfig: AppConfig = AppConfig.load()) {
    install(CallLogging)
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        if (appConfig.allowAnyHost) {
            anyHost()
        } else {
            appConfig.allowedOrigins.forEach { origin ->
                allowHost(origin.host, schemes = origin.schemes)
            }
        }
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    routing {
        get("/health") {
            call.respondText("ok")
        }
        googleAuthRoutes(appConfig.googleClientId)
    }
}
