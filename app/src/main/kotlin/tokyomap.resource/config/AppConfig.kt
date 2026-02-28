package com.tokyomap.resource.config

/**
 * Top-level application configuration.
 */
data class AppConfig(
  val auth: AuthConfig,
  val protectedResource: ProtectedResourceConfig,
  val postgres: PostgresConfig,
)

/** Configuration for the upstream authorisation server. */
data class AuthConfig(
  val host: String,
  val introspectionEndpoint: String,
)

/** Credentials this resource server uses to authenticate itself to the auth server. */
data class ProtectedResourceConfig(
  val resourceId: String,
  val resourceSecret: String,
  /** The URL of this resource server, used to validate the `aud` claim of incoming access tokens. */
  val resourceUrl: String,
)

/** Postgres connection settings. */
data class PostgresConfig(
  val host: String,
  val database: String,
  val user: String,
  val password: String,
  val port: Int,
  val schema: String,
)

/**
 * Reads environment variables and builds an [AppConfig].
 */
fun buildAppConfig(): AppConfig {
  // AS
  val authContainer = requireNotNull(System.getenv("AUTH_CONTAINER")) {"Missing environment variable: AUTH_CONTAINER"}

  // RS
  val resourceId = requireNotNull(System.getenv("RESOURCE_ID")) {"Missing environment variable: RESOURCE_ID"}
  val resourceSecret = requireNotNull(System.getenv("RESOURCE_SECRET")) {"Missing environment variable: RESOURCE_SECRET"}
  val resourceUrl = requireNotNull(System.getenv("RESOURCE_URL")) {"Missing environment variable: RESOURCE_URL"}

  // postgres
  val dbHost = requireNotNull(System.getenv("DB_HOST")) {"Missing environment variable: DB_HOST"}
  val dbDatabase = requireNotNull(System.getenv("DB_DATABASE")) {"Missing environment variable: DB_DATABASE"}
  val dbUser = requireNotNull(System.getenv("DB_USER")) {"Missing environment variable: DB_USER"}
  val dbPassword = requireNotNull(System.getenv("DB_PASSWORD")) {"Missing environment variable: DB_PASSWORD"}
  val dbSchema = requireNotNull(System.getenv("DB_SCHEMA")) {"Missing environment variable: DB_SCHEMA"}
  val dbPort = requireNotNull(System.getenv("DB_PORT")) {"Missing environment variable: DB_PORT" }.toInt()

  return AppConfig(
    auth = AuthConfig(
      host = authContainer,
      introspectionEndpoint = "$authContainer/api/v1/introspect",
    ),
    protectedResource = ProtectedResourceConfig(
      resourceId = resourceId,
      resourceSecret = resourceSecret,
      resourceUrl = resourceUrl,
    ),
    postgres = PostgresConfig(
      host = dbHost,
      database = dbDatabase,
      user = dbUser,
      password = dbPassword,
      port = dbPort,
      schema = dbSchema,
    ),
  )
}
