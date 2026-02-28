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
    single { buildAppConfig() }

    // HTTP client
    single { HttpClient(CIO) } // CIO: Coroutine-based NIO Engine

    // database connection pool
    single<DataSource> {
        val pg = get<AppConfig>().postgres
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${pg.host}:${pg.port}/${pg.database}"
            username = pg.user
            password = pg.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
        })
    }

    // Exposed: connects the DSL to the HikariCP pool
    single { Database.connect(get<DataSource>()) }

    // infrastructure
    single { IntrospectionClient(get(), get()) } // get() resolves HttpClient and AppConfig
    single { UserRepository(get()) } // get() resolves Database

    // services
    single { ResourceService(get(), get()) } // get() resolves IntrospectionClient and UserRepository
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
