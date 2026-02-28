package com.tokyomap.resource.infrastructure.client

import com.tokyomap.resource.config.AppConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.util.Base64

/**
 * HTTP client that calls the upstream authorisation server's token introspection endpoint.
 *
 * @param httpClient Ktor HttpClient injected by Koin.
 * @param config     Application configuration supplying the endpoint URL and credentials.
 */
class IntrospectionClient(private val httpClient: HttpClient,  private val config: AppConfig) {

  /**
   * Posts the token to the introspection endpoint, and returns whether it is active.
   * The endpoint is called with HTTP Basic auth using the resource server's credentials.
   *
   * @param token the raw access token string to validate.
   * @return `true` if the auth server considers the token active, `false` otherwise.
   */
  suspend fun introspect(token: String): Boolean {
    val credentials = encodeClientCredentials(
      config.protectedResource.resourceId,
      config.protectedResource.resourceSecret,
    )

    val response = httpClient.post(config.auth.introspectionEndpoint) {
      header(HttpHeaders.Authorization, "Basic $credentials")
      setBody(FormDataContent(Parameters.build {append("token", token)})) // form-encoded body: token=<token>
    }

    // mirror Node.js: if (!introspectionResponse.ok) throw new Error('introspection failed')
    if (!response.status.isSuccess()) {
      throw IllegalStateException("introspection endpoint returned ${response.status.value}")
    }

    // the auth server returns {"active": true, "errorMessage": null} — extract the `active` field
    val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    return body["active"]?.jsonPrimitive?.content?.toBooleanStrict() ?: false
  }

  /**
   * Base64-encodes `percentEncode(clientId):percentEncode(clientSecret)` for HTTP Basic auth.
   *
   * @param clientId
   * @param clientSecret
   * @return
   */
  private fun encodeClientCredentials(clientId: String, clientSecret: String): String {
    // url-encode clientId, clientSecret to turn `:` in them into `%3A`, which distinguish them from the delimiter `:`.
    val encodedCredentials = "${URLEncoder.encode(clientId, "UTF-8")}:${URLEncoder.encode(clientSecret, "UTF-8")}"

    // `Authrization: Basic` value requires Base64 by the HTTP spec (RFC7617), which operates on bytes.
    return Base64.getEncoder().encodeToString(encodedCredentials.toByteArray())
  }
}
