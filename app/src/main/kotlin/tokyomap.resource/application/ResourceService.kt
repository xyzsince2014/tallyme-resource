package com.tokyomap.resource.application

import com.tokyomap.resource.domain.entity.User
import com.tokyomap.resource.domain.valueobject.AccessTokenPayload
import com.tokyomap.resource.infrastructure.client.IntrospectionClient
import com.tokyomap.resource.infrastructure.repository.UserRepository
import com.tokyomap.resource.domain.exception.ForbiddenException
import com.tokyomap.resource.domain.exception.NotFoundException
import com.tokyomap.resource.domain.exception.UnauthorizedException
import kotlinx.serialization.json.*
import java.util.Base64

/**
 * Application service for the `/userinfo` use case.
 *
 * Orchestrates:
 * 1. JWT payload decoding
 * 2. Token introspection (delegates to [IntrospectionClient])
 * 3. Scope validation
 * 4. User lookup (delegates to [UserRepository])
 * 5. Building the userInfo JSON object filtered by granted scopes
 *
 * @param introspectionClient calls the auth server to verify the token is active
 * @param userRepository      fetches the user record from postgres
 */
class ResourceService(private val introspectionClient: IntrospectionClient, private val userRepository: UserRepository) {
  /**
   * Validates incomingToken, and returns a JsonObject of OpenID Connect claims filtered to the scopes granted to the token.
   *
   * @throws UnauthorizedException if the token is missing, malformed, or inactive.
   * @throws ForbiddenException    if the token does not contain the `openid` scope.
   * @throws NotFoundException     if no user matches the token's `sub` claim.
   */
  suspend fun getUserInfo(incomingToken: String): JsonObject {
    // introspect the token
    // `await` automatically thanks to `suspend`, which is equivalent to `async` in JS.
    val isActive = introspectionClient.introspect(incomingToken)
    if (!isActive) {
      throw UnauthorizedException("token inactive")
    }

    val payload = decodeTokenPayload(incomingToken)

    // OIDC defines a special scope `openid`, which controls overall access to the UserInfo endpoint with access token.
    if ("openid" !in payload.scopes) {
      throw ForbiddenException("no openid in scopes")
    }

    // fetch user by sub
    val user = userRepository.getUserBySub(payload.sub) ?: throw NotFoundException("no matching user for sub=${payload.sub}")

    // node.js: scopes.filter(...).reduce((a, s) => ({...a, ...userInfoFactory[s](user)}), {})
    return buildUserInfo(payload.scopes, user)
  }

  /**
   * Decodes the middle payload segment of a JWT, and maps it to AccessTokenPayload.
   *
   * The JWT is not verified here — verification is delegated to the introspection endpoint.
   * This mirrors the Node.js approach:
   * ```js
   * const cushion = incomingToken.split('.');
   * const payload = JSON.parse(base64url.decode(cushion[1]));
   * ```
   */
  private fun decodeTokenPayload(token: String): AccessTokenPayload {
    val parts = token.split(".")

    if (parts.size < 2) {
      throw UnauthorizedException("invalid token format: expected JWT (3 parts)")
    }

    // decodes the base64url-encoded JWT payload segment into a plain JSON string
    val payloadJsonStr = Base64.getUrlDecoder().decode(parts[1]).toString(Charsets.UTF_8)

    // parses the JSON string into a Kotlin objec
    val payloadJson = Json.parseToJsonElement(payloadJsonStr).jsonObject

    return AccessTokenPayload(
      sub = payloadJson["sub"]?.jsonPrimitive?.content ?: throw UnauthorizedException("missing sub claim in token payload"),
      scopes = payloadJson["scopes"]?.jsonArray?.map { it.jsonPrimitive.content} ?: emptyList(),
    )
  }

  /**
   * Builds a JsonObject containing only the claims relevant to scopes.
   *
   * @param scopes
   * @param user
   * @return
   */
  private fun buildUserInfo(scopes: List<String>, user: User): JsonObject = buildJsonObject {
    scopes.forEach {scope ->
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
}
