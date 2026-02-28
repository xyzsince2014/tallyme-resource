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
)

/** Postgres connection settings. */
data class PostgresConfig(
  val host: String,
  val database: String,
  val user: String,
  val password: String,
  val port: Int,
)

/**
 * Reads environment variables and builds an [AppConfig].
 *
 * Corresponds to `config.js` in the Node.js version.
 */
fun buildAppConfig(): AppConfig {
  val authContainer = System.getenv("AUTH_CONTAINER") ?: "http://localhost:8080"

  return AppConfig(
    auth = AuthConfig(
      host = authContainer,
      introspectionEndpoint = "$authContainer/api/v1/introspect",
    ),
    protectedResource = ProtectedResourceConfig(
      resourceId = System.getenv("RESOURCE_ID") ?: "tokyomap-resource-dev",
      resourceSecret = System.getenv("RESOURCE_SECRET") ?: "fuga",
    ),
    postgres = PostgresConfig(
      host = System.getenv("DB_HOST") ?: "localhost",
      database = System.getenv("DB_DATABASE") ?: "postgres",
      user = System.getenv("DB_USER") ?: "postgres",
      password = System.getenv("DB_PASSWORD") ?: "postgres",
      port = System.getenv("DB_PORT")?.toInt() ?: 5432,
    ),
  )
}
