package com.tokyomap.resource.domain.valueobject

/**
 * Decoded payload of the incoming JWT access token.
 *
 * Pure domain object — no framework annotations.
 * Decoded in the infrastructure layer and passed upward to the application service.
 *
 * @property sub    Subject identifier (user ID).
 * @property scopes List of OAuth 2.0 scopes granted to this token.
 */
data class AccessTokenPayload(
  val sub: String,
  val scopes: List<String>,
)
