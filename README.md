# FaceLit backend

Spring Boot REST backend for FaceLit's account access, academic management, environment sessions, facial attendance and notifications. This repository uses Java 17 and Spring Boot 3.4.1, as declared in [pom.xml](pom.xml). Its Java package is `com.FaceLit.backend`.

## Architecture

```text
HTTP / JWT filter -> controllers -> services -> repositories / PostgreSQL
                                      |
                         SMTP, realtime and facial integration

src/main/java/com/FaceLit/backend/
  academic/       Programs, training groups, enrollment and CSV processing
  auth/           Accounts, credentials, roles, recovery and configuration
  config/         Security and application configuration
  environment/    Environments and attendance sessions
  facial/         Enrollment, reset authorization and attendance
  notification/   In-app messages and email dispatch
  realtime/       Authenticated realtime communication
  shared/         Shared exceptions, utilities and infrastructure
src/main/resources/application.yaml
src/main/resources/static/email/   Runtime email assets
src/test/                         Automated tests and isolated configuration
README.md                         Single maintained documentation entry
```

Controllers accept requests and delegate application behavior; services apply rules and coordinate persistence/delivery. The current facial implementation also contains JDBC integration. This README describes that implementation rather than claiming an architectural refactor has already been delivered. Technical comments remain beside the code; separate implementation reports are not application inputs.

## Prerequisites and configuration

Install Java 17 and use the committed `mvnw`/`mvnw.cmd`, or compatible Maven. Prepare PostgreSQL with [FaceLit-DB](https://github.com/FaceID-Proyect-2026/FaceLit-DB). Facial operations and email acceptance require their configured services.

[application.yaml](src/main/resources/application.yaml) is the configuration source. Supply private environment variables or an ignored local `.env`, read by spring-dotenv. Do not overwrite a working local configuration during a checkout update.

| Variable | Purpose |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME`, `DB_PASSWORD` | Database account |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP account; YAML configures Gmail SMTP on port 587 |
| `JWT_SECRET` | Signing key compatible with the JWT implementation |
| `FACIAL_EMBEDDING_URL` | Facial API address; development default is localhost:8090 |
| `FACIAL_EMBEDDING_API_KEY` | Server-side service credential; configure it privately |
| `FACELIT_ACCESS_URL` | Frontend address used in notifications; development default is localhost:8081 |

HTTP uses port 8080. Hibernate is configured with `ddl-auto: validate`, and backend Liquibase execution is disabled. Run the database project's migration procedure before normal startup. Do not use a Hibernate setting change to conceal missing migrations or schema differences.

The currently checked-out YAML/client still has the legacy facial credential fallback; the coordinated security change is delivered separately. Removing generated files does not certify credential rotation or complete all security findings.

## Run and build

From PowerShell in this repository:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd -Dspring.profiles.active=test test
.\mvnw.cmd -Dspring.profiles.active=test package
```

On Unix-like systems, use `./mvnw` with the same arguments; use `bash mvnw` if the checkout's executable bit is unavailable. Configure isolated mail/JWT values when running tests. [application-test.yaml](src/test/resources/application-test.yaml) uses H2 with disposable schemas. It does not certify PostgreSQL-specific behavior or a production deployment. The explicitly enabled PostgreSQL integration test needs its own local verification environment.

## API and access model

The actual controllers and [SecurityConfig](src/main/java/com/FaceLit/backend/config/SecurityConfig.java) define the contract. The JWT filter reads a Bearer token and establishes the user principal/authorities before controller execution. Public authentication routes and authenticated role checks are distinct; application/service ownership checks must also be tested for the flow in question.

| Area | Current route families and examples |
| --- | --- |
| Authentication | `POST /api/auth/login`, `/verify-password`, `/privacy-acceptance-status` |
| Account access/recovery | `/api/auth/complete-access`, `/request-recovery`, `/verify-token`, `/reset-password`, `/change-password` |
| User administration | `/api/admin/users`, `/api/admin/users/{userId}/role` |
| Personal profile | `/api/profile/me`, `/api/profile/configuration` |
| Academic management | `/api/academic/programs`, `/chips`, `/instructors`, `/csv` and `/me` |
| Environment sessions | `/api/environment/environments`, `/sessions`, `/sessions/active` and session exit-password verification |
| Facial enrollment | `/api/facial/me`, `/registration-status`, `/profile-photo`, `/challenge`, `/enrollment`, `/registration-confirmation` |
| Facial reset | `POST /api/facial/reset-requests`, `GET /api/facial/reset-requests/me` |
| Notifications | `/api/notifications`, read/read-all and recipient-bound facial-request resolution |
| Realtime | `/ws/realtime`, with token validation through its existing protocol |

Use request DTOs and controllers for exact method/body requirements. Program/training-group modification and administration are coordinator operations; environment-session routes are instructor operations in the current security configuration. The `/api/academic/me/**` routes provide role-aware self-service reads. This inventory does not introduce missing CRUD operations or grant new roles.

## Academic and environment behavior

Academic management coordinates programs, training groups (`chip` in the database), instructors, apprentices, assignments and change history. CSV endpoints provide the template, upload and pending-transfer decisions. Duplicate checks, active status and transfer handling are defined by their services and DTOs.

Environment sessions hold entry/exit and shutdown times for attendance. They remain part of the application after schedule management was removed. Follow their existing validation and instructor assignment rules; do not infer support for a screen operation that lacks an API contract.

## Facial enrollment and reset authorization

Enrollment status/confirmation and the facial processing service use the schema established by FaceLit-DB. A reset request is associated with the authenticated apprentice; academic membership supplies display context. Coordinators resolve recipient-bound notifications in the app, not by following an unauthenticated email action.

Approval creates a one-use authorization, normally valid for 24 hours (`facial.reset.authorization-hours`). The token is returned to its authenticated owner through the self-service request endpoint, not in email or coordinator notifications. Replacing the existing face requires the approved, unexpired authorization and live capture. The microservice consumes the permission and saves the replacement in one transaction; a failure must leave the previous state intact. Test the coordinated backend/micro/frontend versions before release.

## Notifications, email and troubleshooting

Notification records and email delivery state are separate concerns. Event/recipient information drives in-app rendering and realtime updates; email dispatch records attempts and outcomes through the existing email log. `FACELIT_ACCESS_URL` must resolve from the recipient's device. A localhost link in a received message targets that device, not the server.

Account-access/recovery links and codes follow their existing services; do not distribute fixed sample passwords or verification codes. A database commit does not by itself prove SMTP delivery. Diagnose mail with the dispatch/error logs and configured account, while preserving private values.

Exit reminders use the session's `exit_time`: periodic checks select the relevant attendees and selected instructor during the configured window before exit. They use the EMAIL channel, are excluded from the in-app inbox and are revalidated before delivery/retry. The existing tests cover cancellation, deduplication, late arrivals and isolation of email-only messages. Actual timing and reception still depend on the running scheduler and SMTP provider.

For startup failures, verify environment variables, database availability and schema validation first. For denied requests, inspect the authenticated role and service ownership conditions; broadening permissions is not a cleanup fix. For facial failures, verify private service connectivity, credential compatibility, capture/authorization and the relevant persisted state. Keep raw biometrics, tokens and credentials out of shared diagnostic output.

## Container and deployment

The [Dockerfile](Dockerfile) builds a new JAR from `pom.xml` and `src/`, using `mvn -q -DskipTests clean package`, then copies `target/*.jar` into a Java 17 runtime image. It does not consume the deleted root JAR, repository-local Maven cache, diagnostic logs or developer-specific settings. Run the appropriate tests independently before accepting this branch; this Dockerfile does not prove a passing suite on its own.

```bash
docker build -t facelit-backend:review .
```

Supply runtime environment through the host configuration. The team identified `srv2068863.hstgr.cloud` as its Hostinger target, but source readiness is not proof that the final commit is deployed. Container hardening and health probes belong to the separately preserved HU-MEJ-015 work, not this cleanup branch.

## Repository hygiene and release acceptance

`.gitignore` excludes `.m2/`, local Maven settings, generated JAR/log files and `.tmp/`, including nested generated files. Maven Wrapper scripts/properties remain tracked; an exception permits a wrapper JAR if a future wrapper distribution needs it. Removing current artifacts does not rewrite Git history.

This README is the single maintained explanatory document. Historical notes, reports and screenshots were archived outside the source checkout before removal and remain recoverable through Git history. Runtime email images, source comments, tests and executable configuration remain present.

Before acceptance, record the final commits and applicable CI checks; verify startup, login per role, academic/environment behavior, enrollment/reset, attendance, notifications and SMTP on a coordinated deployment. Unit/H2 tests do not replace PostgreSQL integration or real camera acceptance. Obtain independent review before merging.

Related repositories: [frontend](https://github.com/FaceID-Proyect-2026/FaceLit), [database](https://github.com/FaceID-Proyect-2026/FaceLit-DB), [facial service](https://github.com/FaceID-Proyect-2026/micro-facial).
