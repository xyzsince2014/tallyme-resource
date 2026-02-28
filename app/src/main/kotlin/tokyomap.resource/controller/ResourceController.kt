package com.tokyomap.resource.controller

import com.tokyomap.resource.application.ResourceService
import com.tokyomap.resource.domain.exception.ForbiddenException
import com.tokyomap.resource.domain.exception.NotFoundException
import com.tokyomap.resource.domain.exception.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Registers routes handled by the resource controller.
 *
 * | Method | Path       | Description                              |
 * |--------|------------|------------------------------------------|
 * | GET    | /userinfo  | Returns OpenID Connect userInfo claims   |
 * | POST   | /profiles  | Returns user profiles for given subs     |
 */
fun Route.resourceRoutes() {
  val resourceService by inject<ResourceService>()

  /**
   * Returns the userinfo related to the given token.
   * Standard OIDC endpoint.
   */
  get("/userinfo") {
    val incomingToken = extractToken(call)

    // 403
    if (incomingToken == null || incomingToken == "null") {
      call.respond(HttpStatusCode.Unauthorized)
      // explicitly exits the lambda passed to `get()`
      return@get
    }

    try {
      // delegate token validation to the auth container
      // cf. RFC 7662 (OAuth 2.0 Token Introspection)
      // todo: adding a cache layer (Redis) for token introspection
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

  /**
   * Returns user profiles for given subs.
   * M2M endpoint using JSON body.
   */
  post("/profiles") {
    // extract token strictly from header (No body touch)
    val token = extractTokenFromHeader(call) ?: return@post call.respond(HttpStatusCode.Unauthorized)

    try {
        val request = call.receive<ProfileRequest>()
        val validSubs = request.subs.filterNotNull()

        if (validSubs.isEmpty()) {
          call.respond(HttpStatusCode.OK, emptyList<String>())
          return@post
        }

        val profiles = resourceService.getProfilesBySubs(token, validSubs)

        call.respond(HttpStatusCode.OK, profiles)

    } catch (e: Exception) {
        call.application.log.error("Failed to parse /profiles body: ${e.message}", e)

        val status = when (e) {
            is UnauthorizedException -> HttpStatusCode.Unauthorized
            is ForbiddenException    -> HttpStatusCode.Forbidden
            else                     -> HttpStatusCode.InternalServerError
        }
        call.respond(status, e.message ?: "Internal Server Error")
    }
  }
}

/**
 * Checks the Authorization header first.
 * Falls back to the `accessToken` parameter if the header is absent.
 *
 * @param call The incoming ApplicationCall containing the request headers and query parameters
 * @return The raw token string, or `null`
 */
private suspend fun extractToken(call: ApplicationCall): String? {
  // check Authorization Header
  extractTokenFromHeader(call)?.let {return it}

  // check Query Parameters
  call.request.queryParameters["accessToken"]?.let {return it}

  // check Form-encoded Body Only if Content-Type is application/x-www-form-urlencoded
  if (call.request.contentType().match(ContentType.Application.FormUrlEncoded)) {
    try {
      val postParams = call.receiveParameters()
      postParams["accessToken"]?.let { return it }
    } catch (e: Exception) {
      // ignore parsing failures
    }
  }

  return null
}

/**
 * Strict token extraction for /profiles.
 * Extracts the token ONLY from the Authorization: Bearer header.
 */
private fun extractTokenFromHeader(call: ApplicationCall): String? {
  val keyword = "bearer "
  val authorization = call.request.headers[HttpHeaders.Authorization]
  if (authorization != null && authorization.lowercase().startsWith(keyword)) {
    return authorization.substring(keyword.length)
  }
  return null
}
