## Why the application failed to start

When the application was launched, Spring reported:

> `No qualifying bean of type 'com.example.demorepository.repository.UserRepository' available`

### What caused this
- The main application class `DemoWebApplication` lives in the `com.example.demoweb` package.
- Spring Boot only registers its own package (`com.example.demoweb`) as an auto-configuration base package.
- The JPA repositories and entities live in different modules under `com.example.demorepository.*`.
- Because those packages were not part of the auto-configuration base packages, Spring Data JPA never scanned `UserRepository`, so no repository bean was created.

### How it was fixed
- Explicitly enable repository scanning for the repository module with `@EnableJpaRepositories(basePackages = "com.example.demorepository.repository")` in `DemoWebApplication`.
- Tell JPA where to find the entities via `spring.jpa.packages-to-scan=com.example.demorepository.entity` in `application.properties`.
- The `@SpringBootApplication` scanBasePackages still includes `com.example.demoweb`, `com.example.demoservice`, and `com.example.demorepository` so the rest of the components are discovered.

With these annotations in `DemoWebApplication`, Spring now detects `UserRepository` during startup and the application context loads successfully.
