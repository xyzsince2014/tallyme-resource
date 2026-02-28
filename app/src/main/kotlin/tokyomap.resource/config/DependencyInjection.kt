package com.tokyomap.resource.config

import com.tokyomap.resource.application.ResourceService
import com.tokyomap.resource.infrastructure.client.IntrospectionClient
import com.tokyomap.resource.infrastructure.repository.UserRepository
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import javax.sql.DataSource

/**
 * Declares all application-wide Koin bindings.
 *
 * `single { }` — instantiated once at startup and reused for every request (singleton scope).
 * `factory { }` — a new instance per injection point (use for stateful, per-request objects).
 * `get()` — resolves an already-declared dependency automatically.
 *
 * Add one `single` / `factory` entry per service as the project grows:
 * ```
 * single { UserService(get()) }
 * single { OrderService(get(), get()) }
 * ```
 */
val appModule = module {
  // config
  single {
    buildAppConfig()
  }

  // HTTP client
  single {
    // CIO: Coroutine-based NIO Engine
    HttpClient(CIO)
  }

  // database connection pool
  single<DataSource> {
    val config = get<AppConfig>() // AppConfig
    val pg = config.postgres // PostgresConfig
    val urlWithSchema = "jdbc:postgresql://${pg.host}:${pg.port}/${pg.database}?currentSchema=${pg.schema}"
    val hikariConfig = HikariConfig().apply {
      jdbcUrl = urlWithSchema
      username = pg.user
      password = pg.password
      driverClassName = "org.postgresql.Driver"
      maximumPoolSize = 10
    }
    HikariDataSource(hikariConfig)
  }

  // Exposed: connects the DSL to the HikariCP pool
  single {
    Database.connect(get<DataSource>())
  }

  // infrastructure
  single {
    // get() resolves HttpClient and AppConfig
    IntrospectionClient(get(), get())
  }
  single {
    // get() resolves Database
    UserRepository(get())
  }

  // services
  // resourceUrl (not resourceId) is the `aud` claim value the auth server embeds in tokens
  single {
    ResourceService(get(), get(), get<AppConfig>().protectedResource.resourceUrl)
  }
}

/**
 * Installs the Koin DI plugin and loads appModule.
 *
 * Called once from Application.main — the Spring `@ComponentScan` equivalent.
 */
fun Application.configureDependencies() {
  install(Koin) {
    slf4jLogger() // logs Koin startup info via SLF4J
    modules(appModule)
  }
}
