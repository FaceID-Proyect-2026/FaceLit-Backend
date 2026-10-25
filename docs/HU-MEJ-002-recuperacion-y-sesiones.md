# HU-MEJ-002: recuperación protegida y sesiones revocables

## Alcance

Implementación sobre el backend `hu-38-dev`, base SHA `0ec22cea968d7a66789dc249b9f31fd1720a8d0c`, con integración del frontend y migración aditiva en FaceLit-DB. No se modificó micro-facial ni se rediseñaron pantallas. No se añadió una prohibición de repetir la contraseña actual.

El flujo de bienvenida y las notificaciones existentes se conservan. Las pruebas de AccountAccessIntegrationTest se adaptaron al nuevo contrato de recuperación manteniendo sus escenarios de cambio de correo, bienvenida y notificaciones.

## Capas

- Controller: valida DTO y aplica límites por cuenta.
- RecoveryChallengeService: serializa validación con el bloqueo de credenciales; cuenta intentos, verifica destino y vencimiento, y emite la autorización temporal.
- PasswordRecoveryServiceImpl: conserva solicitud, actualización de contraseña, consumo y notificaciones.
- RecoveryRateLimiter: ventanas persistentes por cuenta e IP; sin nueva dependencia externa ni contador solo en memoria.
- SessionSecurityService: comprueba sesión, propietario, estado de cuenta/credencial y rol actual; cierra una sesión o revoca todas.
- JwtService: firma el identificador `sid` de una sesión concreta y aplica la validación compartida por HTTP y el WebSocket existente.
- Repositories y Liquibase: persistencia y cambios de esquema versionados.
- Frontend: adapta los servicios y hooks existentes; autorización temporal únicamente en memoria.

## Contratos

| Operación | Solicitud | Respuesta |
|---|---|---|
| POST /api/auth/request-recovery | email | 200 y message; mantiene la decisión existente sobre correo no registrado |
| POST /api/auth/verify-token | email, token (código de seis dígitos) | 200, message y resetToken |
| POST /api/auth/reset-password | email, token (resetToken), newPassword, confirmPassword | 200 y message |
| POST /api/auth/logout | Authorization: Bearer JWT | 204; solo cierra la sesión de ese JWT |

El código por sí solo ya no permite restablecer la contraseña. No existe fallback al contrato anterior. La autorización temporal es aleatoria de 256 bits; solo se almacena su SHA-256 y conserva el vencimiento original de la recuperación (no amplía los cinco minutos). Se consume junto con el cambio de contraseña. Verificar un código ya verificado no emite otra autorización.

El frontend no envía esa autorización como parámetro de navegación ni la guarda en almacenamiento persistente. Si se recarga la página de nueva contraseña, hay que reiniciar la recuperación.

Errores: 400 para recuperación/autorización inválida; 409 para código ya verificado; 410 para vencimiento; 429 con Retry-After para bloqueo o exceso de tasa; 401 para sesión inválida/revocada. Un usuario autenticado sin permiso mantiene el tratamiento 403 de Spring Security.

## Límites

- `security.recovery.max-attempts`: 5 por defecto; el fallo que alcanza el máximo devuelve 429.
- `security.recovery.ip-limit`: 30 solicitudes en cinco minutos, sumando los tres endpoints de recuperación.
- `security.recovery.account-limit`: 10 verificaciones/restablecimientos en quince minutos.
- Solicitudes de envío: una por minuto y tres por quince minutos por correo.
- Un reenvío no elimina el bloqueo de un código que aún no vence ni reinicia los límites de cuenta/IP.
- Los contadores persistentes se comparten entre instancias y se limpian periódicamente.

La IP se toma del peer remoto: no se confía directamente en X-Forwarded-For enviado por el cliente. En un despliegue detrás de proxy debe configurarse y probarse el tratamiento de proxies confiables en el servidor/plataforma. Si todos los usuarios llegan con la IP del proxy o de una NAT, compartirán el límite: revisar ese comportamiento y dimensionar los valores antes de publicar. No habilitar confianza indiscriminada en cabeceras reenviadas.

## Revocación

El JWT contiene `sid`, enlazado a security.user_session. Logout cierra solo esa sesión. Cambio de rol, desactivación por administración, desactivación de instructor, recuperación/cambio de contraseña y actualización de credenciales del flujo de acceso revocan las sesiones anteriores en la transacción correspondiente.

La comprobación de cuenta y rol también ocurre al validar el token. La creación de un nuevo login no vuelve válidos tokens anteriores. Reducir únicamente la expiración del JWT no sustituye este control.

Los JWT anteriores sin sid se rechazan después de actualizar el backend: los usuarios deberán iniciar sesión de nuevo una vez. El cierre por treinta minutos de inactividad no forma parte de esta HU.

## Base de datos y despliegue

Nuevo changeset: `facelit-hu-mej-002-recovery-session-security`, script `01_ddl/04_alter/023_recovery_session_security.sql` y rollback espejo. Añade campos a password_recovery, índice de recuperaciones activas y tabla auth_rate_limit. No modifica el contenido de changesets previamente aplicados.

La migración consume códigos pendientes anteriores porque no tienen destino/autorización del contrato nuevo. El rollback no los reactiva. No elimina usuarios, contraseñas, sesiones históricas ni registros académicos.

Orden para un entorno local de prueba, desde FaceLit-DB, con Docker Desktop iniciado:

```powershell
docker compose up -d postgres
docker compose run --rm liquibase validate
docker compose run --rm liquibase update
```

Comprobar primero el destino de conexión: estos comandos usan el Compose local, no equivalen a actualizar Render. En remoto se aplica el proceso Liquibase ya acordado con la responsable de datos y sin publicar credenciales.

Después desplegar backend y frontend coordinadamente. Una versión vieja del frontend no envía el contrato requerido. No se aplica migración automáticamente mediante Hibernate: se conserva ddl-auto validate.

Backend, desde FaceLit-Backend, con Java y el entorno de desarrollo configurados:

```powershell
.\mvnw.cmd '-Dspring.profiles.active=test' test
.\mvnw.cmd spring-boot:run
```

En esta máquina existe un repositorio Maven local del proyecto. Para ejecutar sin descargar dependencias:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=.m2/repository' '-Dspring.profiles.active=test' test
```

Frontend, desde FaceLit:

```powershell
npm run web
```

Configurar las URLs y correo mediante las variables existentes. No guardar códigos, JWT, autorizaciones ni contraseñas en capturas o logs de evidencia.

## Prueba manual desde las pantallas existentes

1. Con una cuenta de prueba activa, solicitar recuperación. Debe llegar el código al correo vigente.
2. Ingresarlo. Debe abrir la misma pantalla de nueva contraseña, sin pedir otro código/token al usuario.
3. Guardar una contraseña válida y confirmarla. Debe aparecer la confirmación existente y permitir login con ella.
4. En otra recuperación, ingresar cinco códigos incorrectos. El quinto debe mostrar bloqueo; el backend responde 429 y Retry-After. Recargar no elimina el bloqueo.
5. Intentar usar un código de otra cuenta. Debe rechazarse sin cambiar ninguna contraseña.
6. Dejar vencer el código y enviarlo. Debe rechazarse por vencimiento.
7. Abrir dos sesiones. Cerrar una; la otra debe continuar. El JWT de la cerrada debe responder 401 al consultar una ruta protegida.
8. Cambiar rol o desactivar una cuenta de prueba. Su JWT previo debe responder 401; una petición rechazada limpia la sesión del frontend.
9. Probar bienvenida por correo, modificación de correo, notificaciones y acceso de los tres roles.

Si falla la conexión al cerrar sesión, el frontend muestra error y permite reintentar; no presenta como exitosa una revocación que el servidor no confirmó. Un 401 de una sesión ya revocada permite completar la limpieza local.

## Evidencia y límites

Pruebas nuevas: RecoverySessionSecurityIntegrationTest (HTTP, cuenta equivocada, bloqueo persistente, expiración, autorización de un solo uso, revocación y concurrencia). Deben acompañarse de la suite de regresión existente.

Las pruebas con H2 no sustituyen la aplicación de Liquibase y la prueba de concurrencia en PostgreSQL ni un recorrido real con SMTP, web y Android. La HU no se declara Done solo con compilación: requiere evidencia, CI y revisión por otra persona según su DoD.

## Resultado de validación local — 24/10/2026

- Suite completa: **135 pruebas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS.
- Comando: `.\mvnw.cmd -o '-Dmaven.repo.local=.m2/repository' '-Dspring.profiles.active=test' test`.
- TypeScript: `tsc --noEmit`, salida 0.
- YAML del changelog y existencia de referencias SQL/rollback: verificados con el parser YAML del frontend.
- Docker Engine no estaba iniciado; no se ejecutó Liquibase ni la migración/rollback contra PostgreSQL.
- No se certificó SMTP real ni se realizó un recorrido visual web/Android. Tampoco se ejecutó CI ni revisión de PR.
- Los cambios continúan locales, sin commit ni despliegue.

Detalle de reportes Surefire:

| Clase | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| com.FaceLit.backend.academic.dto.response.academic.AcademicResponseContractTest | 2 | 0 | 0 | 0 |
| com.FaceLit.backend.academic.dto.response.academic.InstructorResponseDTOTest | 1 | 0 | 0 | 0 |
| com.FaceLit.backend.academic.model.converter.InstructorTypeConverterTest | 1 | 0 | 0 | 0 |
| com.FaceLit.backend.academic.service.serviceImpl.academic.CsvAcademicServiceImplTest | 6 | 0 | 0 | 0 |
| com.FaceLit.backend.auth.AccountAccessIntegrationTest | 18 | 0 | 0 | 0 |
| com.FaceLit.backend.auth.AccountLifecycleNotificationTest | 2 | 0 | 0 | 0 |
| com.FaceLit.backend.auth.InitialCredentialEmailTemplateTest | 7 | 0 | 0 | 0 |
| com.FaceLit.backend.auth.LoginContractTest | 2 | 0 | 0 | 0 |
| com.FaceLit.backend.auth.RecoverySessionSecurityIntegrationTest | 12 | 0 | 0 | 0 |
| com.FaceLit.backend.BackendApplicationTests | 1 | 0 | 0 | 0 |
| com.FaceLit.backend.environment.ExitReminderRepositoryTest | 4 | 0 | 0 | 0 |
| com.FaceLit.backend.environment.ExitReminderServiceTest | 6 | 0 | 0 | 0 |
| com.FaceLit.backend.environment.service.SessionNotificationTest | 3 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.AttendanceCorrectionServiceTest | 4 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.AttendanceMatrixSessionTimesTest | 2 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.ConfirmedAbsenceDeliveryTest | 3 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.FaceResetCooldownIntegrationTest | 2 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.FaceResetCooldownTest | 10 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.FaceResetServiceTest | 9 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.FacialAttendanceNotificationTest | 7 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.FacialEnrollmentControllerTest | 15 | 0 | 0 | 0 |
| com.FaceLit.backend.facial.RemotePadVerifierTest | 2 | 0 | 0 | 0 |
| com.FaceLit.backend.notification.service.EmailFailureDeliveryTest | 3 | 0 | 0 | 0 |
| com.FaceLit.backend.notification.service.ProfileChangeNotificationTest | 5 | 0 | 0 | 0 |
| com.FaceLit.backend.notification.service.serviceImpl.ExitReminderEmailTest | 6 | 0 | 0 | 0 |
| com.FaceLit.backend.notification.service.serviceImpl.NotificationTransactionTest | 2 | 0 | 0 | 0 |
