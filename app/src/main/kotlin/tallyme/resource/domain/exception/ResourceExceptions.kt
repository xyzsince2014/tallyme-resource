package com.tallyme.resource.domain.exception

/**
 * Domain exceptions for the OAuth2/OIDC resource server access control.
 *
 * All three are grouped in one file because they are small and belong to the same
 * domain concern. The controller catches them and maps each to an HTTP status code:
 *
 * | Exception               | HTTP |
 * |-------------------------|------|
 * | [UnauthorizedException] | 401  |
 * | [ForbiddenException]    | 403  |
 * | [NotFoundException]     | 404  |
 */

/**
 * Thrown when the incoming access token cannot be validated.
 * Maps to HTTP 401.
 */
class UnauthorizedException(message: String) : Exception(message)

/**
 * Thrown when the token lacks a required scope.
 * Maps to HTTP 403.
 */
class ForbiddenException(message: String) : Exception(message)

/**
 * Thrown when a referenced resource does not exist.
 * Maps to HTTP 404.
*/
class NotFoundException(message: String) : Exception(message)
