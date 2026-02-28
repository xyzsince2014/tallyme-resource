package com.tokyomap.resource.infrastructure.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Exposed table definition for `t_usr`.
 *
 * Each property maps one column name to a Kotlin type.
 * Exposed uses this object to generate type-safe SQL at compile time.
 */
object TUsr : Table("t_usr") {
  val sub                 = varchar("sub", 256)
  val name                = varchar("name", 256).nullable()
  val familyName          = varchar("family_name", 256).nullable()
  val givenName           = varchar("given_name", 256).nullable()
  val middleName          = varchar("middle_name", 256).nullable()
  val nickname            = varchar("nickname", 256).nullable()
  val preferredUsername   = varchar("preferred_username", 256).nullable()
  val profile             = varchar("profile", 256).nullable()
  val picture             = varchar("picture", 256).nullable()
  val website             = varchar("website", 256).nullable()
  val zoneinfo            = varchar("zoneinfo", 256).nullable()
  val locale              = varchar("locale", 256).nullable()
  val updatedAt           = datetime("updated_at")                   // timestamp in DB → java.time.LocalDateTime
  val email               = varchar("email", 256).nullable()
  val emailVerified       = bool("email_verified")
  val address             = varchar("address", 256).nullable()       // stored as a JSON string
  val phoneNumber         = varchar("phone", 256).nullable()         // column is `phone` in DB
  val phoneNumberVerified = bool("phone_number_verified")
}
