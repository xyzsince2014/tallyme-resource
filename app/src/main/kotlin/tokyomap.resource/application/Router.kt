package com.tokyomap.resource.application

import com.tokyomap.resource.controller.resourceRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

/**
 * Registers all routes for the application.
 *
 * Services are no longer wired here — Koin resolves them automatically
 * inside each controller via `inject<>()`. Add one line per controller
 * as the project grows.
 */
fun Application.configureRouting() {
    routing {
        resourceRoutes()
    }
}
