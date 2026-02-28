package com.tokyomap.resource.domain.valueobject

/**
 * Represents the response from the upstream auth server's token introspection endpoint.
 *
 * Maps directly to the auth server's `IntrospectResponseDto`.
 * Defined by RFC 7662 (OAuth 2.0 Token Introspection).
 *
 * @property active Whether the token is valid and not expired/revoked.
 * @property sub    Subject identifier (user ID). Present when active is true.
 * @property scope  Space-separated list of granted scope (RFC 7662 §2.2). e.g. "openid profile email"
 * @property aud    Intended audience — the resource server this token was issued for (RFC 7519 §4.1.3).
 */
data class IntrospectionResult(
  val active: Boolean,
  val sub: String?,
  val scope: String?,
  val aud: String?,
)
