package com.tokyomap.resource.application

import com.tokyomap.resource.domain.entity.User
import com.tokyomap.resource.infrastructure.client.IntrospectionClient
import com.tokyomap.resource.infrastructure.repository.UserRepository
import com.tokyomap.resource.domain.exception.ForbiddenException
import com.tokyomap.resource.domain.exception.NotFoundException
import com.tokyomap.resource.domain.exception.UnauthorizedException
import kotlinx.serialization.json.*

/**
 * Application service for the `/userinfo` use case.
 *
 * Orchestrates:
 * 1. Token introspection (delegates to [IntrospectionClient])
 * 2. Audience validation
 * 3. Scope validation
 * 4. User lookup (delegates to [UserRepository])
 * 5. Building the userInfo JSON object filtered by granted scope
 *
 * @param introspectionClient calls the auth server to verify the token is active
 * @param userRepository      fetches the user record from postgres
 * @param resourceUrl         the URL of this resource server, matched against the `aud` claim of the introspection response
 */
class ResourceService(private val introspectionClient: IntrospectionClient, private val userRepository: UserRepository, private val resourceUrl: String) {
  /**
   * Validates incomingToken, and returns a JsonObject of OpenID Connect claims filtered to the scope granted to the token.
   *
   * All claims (sub, scope, aud) are sourced from the introspection response (RFC 7662),
   * not from local JWT decoding, so this works with both JWT and opaque tokens.
   *
   * @throws UnauthorizedException if the token is inactive or sub is missing.
   * @throws ForbiddenException    if aud does not match, or if the token does not contain the `openid` scope.
   * @throws NotFoundException     if no user matches the token's `sub` claim.
   */
  suspend fun getUserInfo(incomingToken: String): JsonObject {
    // introspect the token — auth server is the single source of truth for all claims
    // cf. RFC 7662 (OAuth 2.0 Token Introspection)
    val result = introspectionClient.introspect(incomingToken)
    if (!result.active) {
      throw UnauthorizedException("token inactive")
    }

    val sub = result.sub ?: throw UnauthorizedException("missing sub in introspection response")

    // scope is a space-separated string per RFC 7662 §2.2, e.g. "openid profile email"
    val scope = result.scope?.split(" ") ?: emptyList()

    // verify the token was issued for this resource server (RFC 7519 §4.1.3)
    if (result.aud != resourceUrl) {
      throw ForbiddenException("token audience does not include this resource server")
    }

    // OIDC defines a special scope `openid`, which controls overall access to the UserInfo endpoint.
    if ("openid" !in scope) {
      throw ForbiddenException("no openid in scope")
    }

    // fetch user by sub
    val user = userRepository.getUserBySub(sub) ?: throw NotFoundException("no matching user for sub=$sub")

    // node.js: scope.filter(...).reduce((a, s) => ({...a, ...userInfoFactory[s](user)}), {})
    return buildUserInfo(scope, user)
  }

  /**
   * Builds a JsonObject containing only the claims relevant to scope.
   *
   * @param scope
   * @param user
   * @return
   */
  private fun buildUserInfo(scope: List<String>, user: User): JsonObject = buildJsonObject {
    scope.forEach {scope ->
      when (scope) {
        "openid" -> {
          // `put()` adds a key-value pair to the JSON being built
          put("sub", user.sub)
        }
        "profile" -> {
          user.name?.let              {put("name", it)} // JS: if (user.name != null) obj["name"]  = user.name
          user.familyName?.let        {put("familyName", it)}
          user.givenName?.let         {put("givenName", it)}
          user.middleName?.let        {put("middleName", it)}
          user.nickname?.let          {put("nickname", it)}
          user.preferredUsername?.let {put("preferredUsername", it)}
          user.profile?.let           {put("profile", it)}
          user.picture?.let           {put("picture", it)}
          user.website?.let           {put("website", it)}
          user.gender?.let            {put("gender", it)}
          user.birthdate?.let         {put("birthdate", it)}
          user.zoneinfo?.let          {put("zoneinfo", it)}
          user.locale?.let            {put("locale", it)}
          user.updatedAt?.let         {put("updatedAt", it)}
        }
        "email" -> {
          user.email?.let             {put("email", it)}
          user.emailVerified?.let     {put("emailVerified", it)}
        }
        "address" -> {
          // address is a JSON sub-object, not a flat string
          user.address?.let { addr ->
            put("address", buildJsonObject {
              addr.streetAddress?.let {put("streetAddress", it)}
              addr.locality?.let      {put("locality", it)}
              addr.region?.let        {put("region", it)}
              addr.postalCode?.let    {put("postalCode", it)}
              addr.country?.let       {put("country", it)}
            })
          }
        }
        "phone" -> {
          user.phoneNumber?.let          {put("phoneNumber", it)}
          user.phoneNumberVerified?.let  {put("phoneNumberVerified", it)}
        }
      }
    }
  }

  /**
   * Fetches multiple user profiles by their sub claims.
   * This is for M2M communication, typically called by the RP using a Client Credentials grant token to enrich data.
   *
   * @param incomingToken The access token from the calling service (RP)
   * @param subs List of subject identifiers to fetch
   * @return A JsonObject where keys are subs and values are profile objects (name, picture)
   * @throws UnauthorizedException if the token is inactive
   * @throws ForbiddenException    if the audience mismatch or the token lacks 'profile' scope
   */
  suspend fun getProfilesBySubs(incomingToken: String, subs: List<String>): JsonObject {
    // introspect the token
    val result = introspectionClient.introspect(incomingToken)
    if (!result.active) {
      throw UnauthorizedException("token inactive")
    }

    // verify the token audience matches this RS
    if (result.aud != resourceUrl) {
      throw ForbiddenException("token audience mismatch")
    }

    // scope validation for administrative/service-level access
    // For M2M, we check for 'profile' or a specific 'profile:read' scope instead of 'openid'
    val scope = result.scope?.split(" ") ?: emptyList()
    if ("profile" !in scope && "profile:read" !in scope) {
      throw ForbiddenException("insufficient scope for bulk profile access")
    }

    // bulk fetch from the repository
    val users = userRepository.getUsersBySubs(subs)

    // construct the response
    return buildJsonObject {
      users.forEach {user ->
        put(user.sub, buildJsonObject {
          user.name?.let {put("name", it)}
        })
      }
    }
  }
}
