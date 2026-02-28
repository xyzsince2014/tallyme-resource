package com.tokyomap.resource.domain.valueobject

/**
 * @deprecated No longer used. Claims are now sourced from [IntrospectionResult] (the introspection response)
 * rather than from local JWT decoding. This file can be deleted.
 */
data class AccessTokenPayload(
  val sub: String,
  val scope: List<String>,
  val aud: List<String>,
)
