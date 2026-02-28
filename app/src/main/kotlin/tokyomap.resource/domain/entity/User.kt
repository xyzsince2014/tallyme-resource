package com.tokyomap.resource.domain.entity

/**
 * Domain entity representing a registered user.
 *
 * Mapped from the `t_usr` PostgreSQL table (snake_case columns → camelCase fields).
 * All OpenID Connect standard claims are nullable because scope controls which
 * claims are returned — a user row may not have every field populated.
 *
 * Pure business object — no `@Serializable` or any framework annotation.
 */
data class User(
  val sub: String,

  // --- profile scope ---
  val name: String?,
  val familyName: String?,
  val givenName: String?,
  val middleName: String?,
  val nickname: String?,
  val preferredUsername: String?,
  val profile: String?,
  val picture: String?,
  val website: String?,
  val gender: String?,
  val birthdate: String?,
  val zoneinfo: String?,
  val locale: String?,
  val updatedAt: String?,

  // --- email scope ---
  val email: String?,
  val emailVerified: Boolean?,

  // --- address scope ---
  val address: Address?,

  // --- phone scope ---
  val phoneNumber: String?,
  val phoneNumberVerified: Boolean?,
)

/**
 * Postal address as a JSON object stored in the `address` column of `t_usr`.
 *
 * Corresponds to the OpenID Connect `address` claim structure.
 */
data class Address(
  val streetAddress: String?,
  val locality: String?,
  val region: String?,
  val postalCode: String?,
  val country: String?,
)
