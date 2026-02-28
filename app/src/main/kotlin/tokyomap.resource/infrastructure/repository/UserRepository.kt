package com.tokyomap.resource.infrastructure.repository

import com.tokyomap.resource.domain.entity.Address
import com.tokyomap.resource.domain.entity.User
import com.tokyomap.resource.infrastructure.table.TUsr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Repository which fetches user records from the `t_usr` table using the Exposed DSL.
 *
 * @param database Exposed Database injected by Koin — backed by a HikariCP pool.
 */
class UserRepository(private val database: Database) {

  private val log = LoggerFactory.getLogger(UserRepository::class.java)

  /**
   * Returns the [User] matching [sub], or `null` if not found.
   *
   * @param sub the subject identifier from the access token
   * @return the matching [User], or `null`
   */
  suspend fun getUserBySub(sub: String): User? = withContext(Dispatchers.IO) {
    try {
      // `transaction` opens a JDBC connection from the pool, runs the block, then releases it.
      val user = transaction(database) {
        TUsr.selectAll()
          .where {TUsr.sub eq sub }  // WHERE sub = ?
          .firstOrNull()              // returns the first ResultRow or null
          ?.toUser()                  // maps the ResultRow to a domain User
      }

      if (user == null) {
        log.info("[UserRepository.getUserBySub] no matching usr")
      } else {
        log.info("[UserRepository.getUserBySub] usr = $user")
      }

      user

    } catch (e: Exception) {
      log.error("[UserRepository.getUserBySub] ${e.message}", e)
      throw e
    }
  }

  /**
   * Maps an Exposed ResultRow to a User domain model.
   *
   * @return
   */
  private fun ResultRow.toUser(): User {
    return User(
      // `this` refers to `ResultRow`
      sub                 = this[TUsr.sub],
      name                = this[TUsr.name],
      familyName          = this[TUsr.familyName],
      givenName           = this[TUsr.givenName],
      middleName          = this[TUsr.middleName],
      nickname            = this[TUsr.nickname],
      preferredUsername   = this[TUsr.preferredUsername],
      profile             = this[TUsr.profile],
      picture             = this[TUsr.picture],
      website             = this[TUsr.website],
      gender              = null,                                        // column does not exist in t_usr
      birthdate           = null,                                        // column does not exist in t_usr
      zoneinfo            = this[TUsr.zoneinfo],
      locale              = this[TUsr.locale],
      updatedAt           = this[TUsr.updatedAt].toString(),             // LocalDateTime → String
      email               = this[TUsr.email],
      emailVerified       = this[TUsr.emailVerified],
      address             = this[TUsr.address]?.let {parseAddress(it) },
      phoneNumber         = this[TUsr.phoneNumber],
      phoneNumberVerified = this[TUsr.phoneNumberVerified],
    )
  }

  /**
   * Parses the JSON string stored in the `address` column into an Address domain model.
   *
   * @param json
   * @return
   */
  private fun parseAddress(json: String): Address {
    val obj = Json.parseToJsonElement(json).jsonObject
    return Address(
      streetAddress = obj["street_address"]?.jsonPrimitive?.content,
      locality      = obj["locality"]?.jsonPrimitive?.content,
      region        = obj["region"]?.jsonPrimitive?.content,
      postalCode    = obj["postal_code"]?.jsonPrimitive?.content,
      country       = obj["country"]?.jsonPrimitive?.content,
    )
  }

  /**
   * Returns a list of [User] domain models matching the given [subs].
   *
   * @param subs list of subject identifiers to fetch
   * @return list of matching [User] objects (may be empty if no matches found)
   */
  suspend fun getUsersBySubs(subs: List<String>): List<User> = withContext(Dispatchers.IO) {
    if (subs.isEmpty()) {
      return@withContext emptyList()
    }

    try {
      val users = transaction(database) {
        val rows = TUsr.selectAll().where {TUsr.sub inList subs}
        return@transaction rows.map {it.toUser()}
      }

      log.info("[UserRepository.getUsersBySubs] found ${users.size} matching users")

      return@withContext users

    } catch (e: Exception) {
      log.error("[UserRepository.getUsersBySubs] ${e.message}", e)
      throw e
    }
  }
}
