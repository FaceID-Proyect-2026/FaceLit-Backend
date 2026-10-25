# FaceLit backend

Spring Boot REST backend for FaceLit academic management, authentication, environment sessions, facial attendance and notifications. The actual Java package is `com.FaceLit.backend`.

## Requirements

- Java 17.
- Spring Boot 3.4.1, as declared in [pom.xml](pom.xml).
- Maven through the committed `mvnw` / `mvnw.cmd` wrapper, or an installed Maven compatible with this project.
- PostgreSQL with the FaceLit-DB migrations applied, and access to the configured mail and facial services for their respective flows.

## Configuration and startup

[application.yaml](src/main/resources/application.yaml) is the configuration source. Supply environment variables or a local ignored `.env` (loaded by spring-dotenv). Obtain private values from the environment owner rather than committing them.

| Variable | Purpose |
| --- | --- |
| DB_URL | PostgreSQL JDBC URL |
| DB_USERNAME / DB_PASSWORD | Database account |
| MAIL_USERNAME / MAIL_PASSWORD | SMTP account; the current YAML configures Gmail SMTP on port 587 |
| JWT_SECRET | Signing secret compatible with the configured JWT implementation |
| FACIAL_EMBEDDING_URL | Facial service URL; defaults to http://localhost:8090 |
| FACIAL_EMBEDDING_API_KEY | Server-side service credential; configure a real environment value rather than relying on the development fallback |
| FACELIT_ACCESS_URL | Frontend access link used in notifications; defaults to http://localhost:8081 |

The default HTTP port is 8080. Hibernate uses `ddl-auto: validate`; Liquibase is disabled in this application. Apply migrations from [FaceLit-DB](https://github.com/FaceID-Proyect-2026/FaceLit-DB) before normal startup. This backend does not create or migrate the installed production schema on startup.

From PowerShell on Windows:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd -Dspring.profiles.active=test test
.\mvnw.cmd -DskipTests package
```

On Unix-like systems use `./mvnw` with the same arguments. The test profile uses an isolated H2 database from [application-test.yaml](src/test/resources/application-test.yaml); it does not validate a production PostgreSQL deployment. Tests may need isolated values for mail and JWT settings. A skipped-test package or compile is not a passing test suite.

## Container deployment

The [Dockerfile](Dockerfile) builds from `pom.xml` and `src/` with `mvn -q -DskipTests clean package`, then copies `target/*.jar` into a Java 17 runtime image. It does not consume `FaceID-Back-end.jar`, repository-local `.m2/`, diagnostic logs or `.mvn/local-settings.xml`. The Maven wrapper configuration remains versioned for local development.

Build with `docker build -t facelit-backend .`. Supply deployment environment variables through the hosting platform. Inspect the running deployment logs and verify login and required API flows before acceptance; a documented container build path alone does not prove a successful Render deployment.

## Source layout

```text
src/main/java/com/FaceLit/backend/
  academic/       Academic programs, training groups and enrollment
  auth/           Authentication and account access
  config/         Application and security configuration
  environment/    Environments and attendance sessions
  facial/         Facial integration and attendance
  notification/   Notification delivery
  realtime/       Realtime communication
  shared/         Shared infrastructure
src/main/resources/application.yaml
src/test/         Automated tests and isolated test configuration
docs/             Technical documentation and implementation records
```

## Documentation and related repositories

- [Documentation inventory](docs/documentation-inventory.md), including the exact source-to-destination map for documents moved out of Java packages.
- [Module documentation](docs/modules/README.md). Preserved implementation records require detailed reconciliation under HU-MEJ-018 before being treated as current API contracts.
- [Frontend](https://github.com/FaceID-Proyect-2026/FaceLit), [database](https://github.com/FaceID-Proyect-2026/FaceLit-DB) and [facial service](https://github.com/FaceID-Proyect-2026/micro-facial).

HU-MEJ-016 removes generated files from the current tree. Existing Git history is retained. Business logic, permissions and migrations are outside this cleanup.
