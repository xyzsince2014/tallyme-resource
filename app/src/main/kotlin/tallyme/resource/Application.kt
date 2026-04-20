package com.tallyme.resource

import com.tallyme.resource.application.configureRouting
import com.tallyme.resource.config.configureDependencies
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*  // embeddedServer, ApplicationEngine — core server lifecycle API
import io.ktor.server.netty.* // Netty engine adapter — provides the NIO-based HTTP server backend
import io.ktor.server.plugins.contentnegotiation.*

/**
 * The entry point of the tallyme-resource Ktor application.
 * Uses Netty (HTTP engine) as the underlying HTTP server.
 *
 * Bootstrap order matters:
 * 1. [configureDependencies] — installs Koin and builds the DI object graph
 * 2. [ContentNegotiation]    — installs JSON serialization
 * 3. [configureRouting]      — registers all routes (controllers resolve services via Koin)
 */
fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        configureDependencies()
        install(ContentNegotiation) {
            json() // (de)serialize request and response bodies as JSON
        }
        configureRouting()
    }.start(
        // `wait = true` keeps the process running
        wait = true
    )
}
