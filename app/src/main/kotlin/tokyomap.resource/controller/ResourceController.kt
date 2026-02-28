package com.tokyomap.resource.controller

import com.tokyomap.resource.application.ResourceService
import com.tokyomap.resource.domain.exception.ForbiddenException
import com.tokyomap.resource.domain.exception.NotFoundException
import com.tokyomap.resource.domain.exception.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Registers routes handled by the resource controller.
 *
 * | Method | Path       | Description                              |
 * |--------|------------|------------------------------------------|
 * | GET    | /userinfo  | Returns OpenID Connect userInfo claims   |
 */
fun Route.resourceRoutes() {
  val resourceService by inject<ResourceService>()

  // note that this block is a lambda
  get("/userinfo") {
    val incomingToken = extractToken(call)

    // 403
    if (incomingToken == null || incomingToken == "null") {
        call.respond(HttpStatusCode.Unauthorized)
      return@get // explicitly exits the lambda passed to `get()`
    }

    try {
      // delegate token validation to service
      val userInfo = resourceService.getUserInfo(incomingToken)
      call.application.log.info("[resourceRoutes.getUserInfo] userInfo = $userInfo")
      call.respond(HttpStatusCode.OK, userInfo)

    } catch (e: Exception) {
      val status = when (e) {
        is UnauthorizedException -> HttpStatusCode.Unauthorized
        is ForbiddenException    -> HttpStatusCode.Forbidden
        is NotFoundException     -> HttpStatusCode.NotFound
        else                     -> HttpStatusCode.InternalServerError
      }
      call.application.log.warn("[resourceRoutes.getUserInfo] ${status.description}: ${e.message}")
      call.respond(status)
    }
  }
}

/**
 * Checks the `Authorization: Bearer <token>` header first.
 * Falls back to the `accessToken` query parameter if the header is absent.
 *
 * @param call The incoming ApplicationCall containing the request headers and query parameters
 * @return The raw token string, or `null`
 */
private fun extractToken(call: ApplicationCall): String? {
  // fetch from `Authorization: Bearer <token>` header
  val authorization = call.request.headers[HttpHeaders.Authorization]
  if (authorization != null && authorization.lowercase().startsWith("bearer ")) {
    return authorization.substring("bearer ".length)
  }

  // fetch from query param `accessToken=<token>` otherwise
  return call.request.queryParameters["accessToken"]
}
