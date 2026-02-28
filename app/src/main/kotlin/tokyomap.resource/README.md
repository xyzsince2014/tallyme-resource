```txt
src/main/kotlin/com/tokyomap/
 ├─ application/                # use‑case / service layer
 │    └─ UserInfoUseCase.kt
 ├─ domain/
 │    ├─ model/                 # entities & value objects
 │    │   ├─ User.kt
 │    │   ├─ AccessToken.kt
 │    │   └─ Scope.kt          # maybe an enum/aggregate
 │    ├─ repository/
 │    │   └─ UserRepository.kt # interface only
 │    └─ service/
 │         └─ TokenIntrospectionService.kt
 ├─ infrastructure/
 │    ├─ persistence/
 │    │   └─ PgUserRepository.kt   # implements UserRepository with R2DBC/Exposed
 │    └─ auth/                     
 │         └─ KtorTokenProvider.kt # uses ktor-client to call introspection
 ├─ config/
 ├─ controller/                  # Ktor routing, maps HTTP → use cases
 └─ util/
```
