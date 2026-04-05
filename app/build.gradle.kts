// Gradle requires plugins to come first
plugins {
  kotlin("jvm") version "1.9.24"
  kotlin("plugin.serialization") version "1.9.24" // enables @Serializable for kotlinx.serialization
  application
  id("com.github.johnrengelman.shadow") version "8.1.1" // produces a fat JAR with all dependencies bundled
}

// pre-existing properties
group = "tokyomap-resource"
version = "2.0.0"

repositories {
  mavenCentral()
}

val ktorVersion = "2.3.12"
val koinVersion = "3.5.6"

dependencies {
  // --- server ---
  implementation("io.ktor:ktor-server-core:$ktorVersion") // core server API (embeddedServer, ApplicationEngine, etc.)
  implementation("io.ktor:ktor-server-netty:$ktorVersion") // Netty engine adapter
  implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion") // Content-Type negotiation plugin
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion") // JSON serialization via kotlinx.serialization

  // --- HTTP client used internally for token introspection) ---
  implementation("io.ktor:ktor-client-core:$ktorVersion") // Ktor HTTP client core
  implementation("io.ktor:ktor-client-cio:$ktorVersion") // CIO (coroutine-based NIO) engine for the HTTP client
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion") // content negotiation plugin for the HTTP client

  // --- database ---
  implementation("org.postgresql:postgresql:42.7.3") // postgres JDBC driver
  implementation("com.zaxxer:HikariCP:5.1.0") // HikariCP connection pool (equivalent to pg.Pool in Node.js)
  implementation("org.jetbrains.exposed:exposed-core:0.50.1") // Exposed DSL core (Table definitions, select/insert/update/delete)
  implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1") // Exposed JDBC backend (bridges DSL to JDBC / HikariCP)
  implementation("org.jetbrains.exposed:exposed-java-time:0.50.1") // Exposed support for java.time types (LocalDateTime etc.)

  // --- DI ---
  implementation("io.insert-koin:koin-ktor:$koinVersion") // Koin DI integration for Ktor
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion") // Koin startup logging via SLF4J

  // --- logging ---
  implementation("ch.qos.logback:logback-classic:1.5.6") // SLF4J implementation — required for any log output

  // --- test ---
  testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") // Ktor embedded test engine (testApplication, client, etc.)
  testImplementation("io.mockk:mockk:1.13.12") // MockK — coroutine-aware mocking library
  testImplementation(kotlin("test")) // kotlin-test assertions + JUnit 5 integration
}

// the entry point when running the JAR
application {
  mainClass.set("com.tokyomap.resource.ApplicationKt")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}
