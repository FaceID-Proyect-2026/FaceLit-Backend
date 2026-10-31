# HU-46 — Coordinador inicial sin credenciales fijas

Actualizado: 31 de octubre de 2026. Proyecto: FaceLit, equipo 4. Referencia: punto 2 del plan de la evaluación E2E del 30 de octubre, H-05/H-27 de ese informe (numeración propia de esa evaluación).

## Objetivo y alcance

Retirar la cuenta predefinida del seed y permitir que un operador autorizado cree el primer coordinador o rote la contraseña de uno existente mediante configuración privada. No se añade registro público, no se elevan cuentas ordinarias y no se modifican las reglas académicas, faciales ni de asistencia.

Estado de entrega: implementación local con pruebas automatizadas aprobadas. El 31 de octubre, después de orientar la creación en una base sin resultados en la consulta de coordinador, la usuaria informó que el proceso funcionó. Esa confirmación no incluye aquí el log final ni acredita una rotación en otros ambientes: QA debe adjuntar el resultado CREATED/ROTATED y verificar el ingreso. No se ha verificado Hostinger ni se ha reescrito el historial de Git. Este documento solicita revisión; no declara aprobación independiente.

## Ramas y trazabilidad

| Repositorio | Rama local | HEAD base verificado al preparar esta entrega |
|---|---|---|
| FaceLit-Backend | `hu-46-dev` | `2ab0bcd6b46d6ce9620570bd09a2ca7521461286` |
| FaceLit-DB | `hu-46-main` | `d2879d7330f995e7ee1f854b72d87024cd9a09d2` |

Estos SHA identifican las bases usadas durante la implementación, no los commits de entrega. Para la publicación solicitada el 31 de octubre, consultar los commits de las ramas HU y registrar sus SHA y la URL del PR antes de revisar. Las ramas también contienen cambios del punto 1 sobre sesiones; esos archivos no forman parte del alcance de este documento. Frontend y microservicio no requieren cambios para el punto del coordinador.

## Problema localizado

El changeset histórico `02-seed-coordinator` ejecutaba un SQL con identidad fija, un bcrypt versionado y un comentario con una contraseña anterior. Quitar el archivo no invalida por sí solo una credencial instalada ni las sesiones emitidas con ella.

Durante la integración también se encontró que el login ejecutaba un `ALTER TABLE` para `initial_password_prompt_seen`, columna ya definida en el esquema y la migración 016. Eso mezclaba evolución de esquema y autenticación, y en H2 confirmaba prematuramente transacciones de prueba. Se mapeó el campo existente en `User` y se retiró ese DDL del login; JPA conserva la misma regla de mostrar el aviso inicial una sola vez. No se cambió la política de obligatoriedad del cambio de contraseña.

La primera regresión completa detectó que `BackendApplicationTests` no elegía un perfil de pruebas y arrancaba contra la base habitual. El control rechazó allí la credencial retirada. Esa prueba ahora selecciona explícitamente `test`, con H2 y valores sintéticos de JWT/correo; el control de seguridad sigue activo. No se deshabilitó la prueba ni se renovó la cuenta real para ocultar el fallo.

## Decisiones de implementación

| Responsabilidad | Ubicación |
|---|---|
| Entrada administrativa y validación de formato | `auth/dto/request/security/CoordinatorBootstrapRequestDTO.java` |
| Reglas, transacción, creación, rotación y detección de credencial retirada | `auth/service/security/CoordinatorBootstrapService.java` y `auth/service/serviceImpl/security/CoordinatorBootstrapServiceImpl.java` |
| Bloqueo de rol para inicialización concurrente | `auth/repository/roleandpermission/RoleRepository.java` |
| Invalidación de recuperaciones pendientes | `auth/repository/security/PasswordRecoveryRepository.java` |
| Sesiones y solicitudes de acceso | Servicios existentes `SessionSecurityService` y `AccountAccessService` |
| Lectura de configuración y operación al arrancar | `config/CoordinatorBootstrapProperties.java`, `config/CoordinatorBootstrapConfiguration.java` y `application.yaml` |
| Captura privada de contraseña para una operación puntual | `scripts/initialize-coordinator.ps1` |
| Campo persistente del aviso inicial y login sin DDL | `auth/model/security/User.java`, `auth/service/serviceImpl/roleandpermission/LoginServiceImpl.java` |

Se mantienen las capas y las carpetas actuales. Configuración inicia el caso de uso; el servicio decide; los repositorios persisten. No se introducen dependencias, tablas, frameworks ni endpoints nuevos. La contraseña se almacena con el `PasswordEncoder` BCrypt existente, y la validación reutiliza la política de cambio de contraseña de la aplicación. El DTO oculta sus datos al convertirlo a texto; el log de éxito solo indica CREATED, ROTATED o UNCHANGED.

### Creación y repetición

- Requiere documento válido, nombre, apellido, correo y contraseña privada.
- Solo crea el primer coordinador si no hay uno existente. No transforma un aprendiz o instructor en coordinador.
- Si la misma cuenta ya existe con la misma credencial, no la duplica ni vuelve a generar su hash.
- Un cambio sobre una credencial existente exige habilitar expresamente la rotación.

### Rotación

- Requiere un coordinador activo, su documento y el correo que ya tiene registrado.
- Mantiene su UUID, datos personales, rol y relaciones históricas; no reactiva ni promueve cuentas.
- Cambia el hash, reinicia el bloqueo por intentos, revoca las sesiones, invalida recuperaciones y cancela solicitudes de acceso pendientes que pudieran enviar la credencial anterior.
- Si permanece exactamente el bcrypt publicado por la semilla retirada en un coordinador activo, el arranque falla con una indicación de rotación. La detección usa una huella SHA-256 no utilizable para autenticación; no se vuelve a incluir el bcrypt ni su contraseña en el código.
- Esta huella detecta ese valor concreto. No certifica que todas las contraseñas alguna vez expuestas hayan sido renovadas.

## Tratamiento de Liquibase y archivos retirados

En FaceLit-DB se retiró la entrada obsoleta de `02_dml/00_inserts/0000changelog.yaml` y se eliminaron del árbol activo:

- `02_dml/00_inserts/002_seed_coordinator.sql`.
- `05_rollbacks/02_dml/00_inserts/002_seed_coordinator.rollback.sql`.

El retiro es deliberado: no se sustituye el SQL histórico por otro contenido bajo el mismo ID. Las bases ya migradas conservan su fila y su checksum en `DATABASECHANGELOG`, y las cuentas instaladas permanecen. El seed de roles/permisos y los demás SQL de esquema no se editan. No se ejecuta `clearCheckSums`, no se borra la base y no se elimina un coordinador como supuesto rollback. El antiguo changeset de cuenta deja de ser un objetivo de rollback en el changelog activo; cualquier recuperación histórica debe revisarse con respaldo y con su versión correspondiente.

La prueba reproducible aplica el baseline anterior y después el changelog actual, comprueba la conservación de esa fila/checksum y verifica que una instalación nueva no cree ninguna cuenta. El historial de Git sigue conteniendo el material anterior: la corrección requiere rotación, no solamente limpieza del árbol.

## Inventario de archivos de este punto

Las rutas de Java de la siguiente tabla parten de `FaceLit-Backend/src/main/java/com/FaceLit/backend/`; las de pruebas parten de `FaceLit-Backend/src/test/java/com/FaceLit/backend/`.

| Estado | Archivo o archivos | Cambio |
|---|---|---|
| Nuevo | `auth/dto/request/security/CoordinatorBootstrapRequestDTO.java` | Entrada validada; conversión a texto sin datos de la cuenta. |
| Nuevos | `auth/service/security/CoordinatorBootstrapService.java`, `auth/service/serviceImpl/security/CoordinatorBootstrapServiceImpl.java` | Caso de uso de creación/rotación, transacción y detección de la credencial retirada. |
| Nuevos | `config/CoordinatorBootstrapProperties.java`, `config/CoordinatorBootstrapConfiguration.java` | Configuración privada y ejecución al arrancar. |
| Modificado | `auth/repository/roleandpermission/RoleRepository.java` | Bloqueo de rol para serializar la inicialización. |
| Modificado | `auth/repository/security/PasswordRecoveryRepository.java` | Invalidación de autorizaciones pendientes al rotar. |
| Modificado | `auth/model/security/User.java` | Mapeo de la columna existente del aviso inicial. |
| Modificado | `auth/service/serviceImpl/roleandpermission/LoginServiceImpl.java` | Retiro del DDL en tiempo de login; uso del campo persistido. |
| Nuevos, pruebas | `auth/CoordinatorBootstrapIntegrationTest.java`, `auth/CoordinatorBootstrapRunnerTest.java`, `auth/CoordinatorBootstrapPostgresIT.java` | Cobertura de creación, rotación, rechazos, arranque y compatibilidad PostgreSQL. |
| Modificados, pruebas | `BackendApplicationTests.java`, `notification/service/serviceImpl/NotificationTransactionTest.java` | Perfil de pruebas aislado y ajuste del constructor del login. |

| Estado | Ruta desde el repositorio indicado | Cambio |
|---|---|---|
| Modificado | Backend: `src/main/resources/application.yaml` | Propiedades del inicializador, deshabilitado por defecto. |
| Nuevo | Backend: `.env.example` | Plantilla sin credenciales reales. El `.env` local no se reemplazó. |
| Nuevo | Backend: `scripts/initialize-coordinator.ps1` | Contraseña oculta, operación puntual, restauración de variables y DevTools desactivado en la JVM hija. |
| Modificado | Backend: `README.md` | Uso y restricciones del inicializador. |
| Nuevo | Backend: `docs/HU46_COORDINADOR_QA.md` | Este informe y procedimiento de revisión. |
| Modificado | DB: `02_dml/00_inserts/0000changelog.yaml` | Retiro de la entrada de cuenta fija; conserva el seed de roles/permisos. |
| Eliminado | DB: `02_dml/00_inserts/002_seed_coordinator.sql` | Retiro de la identidad y credencial fijas del árbol activo. |
| Eliminado | DB: `05_rollbacks/02_dml/00_inserts/002_seed_coordinator.rollback.sql` | Retiro del rollback asociado a la cuenta fija; no se ejecutó para borrar usuarios. |
| Nuevo | DB: `scripts/test-coordinator-bootstrap.ps1` | Dos escenarios de PostgreSQL temporal y comprobación del historial Liquibase. |
| Modificado | DB: `README.md` | Nuevo procedimiento y precauciones de migración. |

`SessionSecurityService` y `AccountAccessService` se reutilizan, no se modifican en este punto. No se reorganizan carpetas, no se separa la base compartida por las dos APIs y no se cambia el flujo de reconocimiento facial.

## Cómo ejecutarlo en un ambiente real

1. Preparar las variables privadas de conexión y servicios del backend; `.env.example` solo tiene marcadores vacíos y opciones de inicialización deshabilitadas.
2. Aplicar el changelog actual de FaceLit-DB siguiendo su procedimiento normal, con respaldo previo en ambientes con datos importantes.
3. Desde `FaceLit-Backend`, elegir una sola operación y reemplazar los marcadores:

Requisitos: JDK compatible con el proyecto (Java 17), Maven Wrapper y conexión a PostgreSQL con las migraciones y el catálogo COORDINATOR aplicados. El backend no ejecuta Liquibase automáticamente en su configuración principal. Para las pruebas temporales se necesita además Docker activo y el puerto `55447` disponible. Usar un entorno de QA preparado; no vaciar la base habitual para repetir la creación.

Antes de elegir modo, confirmar que `DB_URL` del backend corresponde al host, puerto y base consultados en pgAdmin. Una consulta vacía con JOIN no prueba por sí sola que no existan usuarios. Consultar también:

```sql
SELECT COUNT(*) AS usuarios FROM security.user_app;
SELECT role_name FROM roleandpermission.role;
```

Si no hay usuarios y existe COORDINATOR, usar Create con los datos que se quieren registrar. Si ya existe un coordinador, usar Rotate con su documento y correo actuales, no con otros datos. Si hay usuarios pero la consulta de coordinador está vacía, revisar las asignaciones antes de crear cuentas.

```powershell
# Solo instalación nueva sin coordinador:
.\scripts\initialize-coordinator.ps1 -Mode Create -Document "<documento>" -Email "<correo>" -FirstName "<nombre>" -LastName "<apellido>"

# Coordinador existente; usar su documento y correo actuales:
.\scripts\initialize-coordinator.ps1 -Mode Rotate -Document "<documento-actual>" -Email "<correo-actual>"
```

El script pide contraseña y confirmación con entrada oculta. No hay que escribirlas en los argumentos, el chat ni el repositorio. La política existente exige 8 a 15 caracteres, mayúscula, número y símbolo permitido. El proceso temporal utiliza un puerto aleatorio en loopback y se cierra al terminar; las variables del proceso se restauran. No escribe la contraseña en `.env`.

Seguimiento del 31 de octubre: una ejecución rechazó la rotación porque el correo no coincidía con la cuenta identificada por documento; no renovó la credencial. Más adelante la consulta compartida no devolvió coordinador y se orientó el uso de Create, tras comprobar la base y el catálogo. La usuaria posteriormente informó que funcionó. Se ajustó el script para desactivar DevTools mediante una propiedad de la JVM hija: el reiniciador había mostrado `BUILD SUCCESS` después de un fallo del ApplicationRunner. Se comprobó la sintaxis del ajuste; los resultados automatizados del día 30 preceden ese ajuste del script. Un `BUILD SUCCESS` aislado no acredita la operación: debe aparecer CREATED o ROTATED según el caso y verificarse el login posterior.

4. Mantener la inicialización y rotación deshabilitadas en la configuración habitual. Iniciar el backend normalmente e ingresar con la nueva contraseña. Las sesiones anteriores deben volver al login.
5. Adjuntar evidencia sin contraseñas, hashes ni JWT: ambiente, fecha, resultado de la operación y verificación de acceso. Repetir la rotación en cada ambiente que haya usado la credencial expuesta.

Consulta de identificación, sin mostrar hashes:

```sql
SELECT u.id_user_app, u.number_document, c.email, u.account_status, c.credential_status
FROM security.user_app u
JOIN security.credential c ON c.id_user_app = u.id_user_app
JOIN roleandpermission.user_role ur ON ur.id_user_app = u.id_user_app
JOIN roleandpermission.role r ON r.id_role = ur.id_role
WHERE r.role_name = 'COORDINATOR';
```

Después de una creación o rotación exitosa, iniciar normalmente desde `FaceLit-Backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

El backend debe permanecer activo en `8080` sin `Application run failed`. Abrir el frontend que usa esa API y entrar con el documento y la contraseña elegida. No usar la contraseña aleatoria que Spring imprime como aviso de desarrollo. La terminal se usa para esta operación administrativa; no para cada inicio de sesión. No es necesario insertar usuarios a mano ni cambiar contraseñas directamente mediante SQL.

## Casos manuales para QA

Probar los casos negativos y la rotación en cuentas de QA, no sobre una cuenta de trabajo ajena. No incluir contraseñas ni JWT en capturas.

| Caso | Acción | Resultado esperado |
|---|---|---|
| Creación desde cero | En base preparada sin coordinador, ejecutar Create y consultar la cuenta/rol. | CREATED, una cuenta y su rol COORDINATOR; no existe contraseña predeterminada. |
| Acceso visual | Arrancar backend y entrar desde el frontend con la cuenta creada. | Login correcto y funciones de coordinación según sus permisos; el backend sigue activo. |
| Repetición | Repetir Create con la misma identidad y contraseña. | UNCHANGED, sin cuenta duplicada ni regeneración del hash. |
| Segunda cuenta inicial | Intentar Create con otro documento cuando ya existe el coordinador. | Rechazo; no se crea otro coordinador inicial. |
| Formato/contraseña | Probar documento inválido, contraseña débil o confirmación diferente. | Rechazo claro, sin guardar cambios ni imprimir la contraseña. |
| Correo incorrecto | Rotate con documento correcto y otro correo. | Rechazo; la contraseña anterior sigue sin cambios. |
| Cuenta ordinaria | Intentar inicialización usando el documento de un aprendiz/instructor. | Rechazo; se conserva su rol. |
| Rotación | Con sesión de QA abierta, Rotate con documento/correo actuales y una contraseña diferente. | ROTATED; UUID y relaciones se conservan; contraseña anterior rechazada y nueva aceptada. |
| Sesiones y recuperación | Después de rotar, comprobar JWT anterior y recuperaciones pendientes mediante las pruebas HTTP incluidas. | JWT anterior recibe 401; la autorización de recuperación anterior deja de ser válida. |
| Credencial retirada | Ejecutar el escenario automatizado contra el baseline anterior. | Se detecta el bcrypt retirado; la actualización conserva el historial y la rotación habilita el acceso nuevo. |

La huella de detección corresponde al bcrypt concreto del seed retirado. No es un análisis universal de contraseñas expuestas ni verifica si una persona reutiliza esa contraseña bajo otro hash.

## Pruebas y revisión

```powershell
# Backend: integración y arranque.
.\mvnw.cmd "-Dtest=CoordinatorBootstrapIntegrationTest,CoordinatorBootstrapRunnerTest,NotificationTransactionTest" test

# Backend: regresión completa.
.\mvnw.cmd test

# Desde FaceLit-DB: bases nuevas y actualización de un baseline anterior.
.\scripts\test-coordinator-bootstrap.ps1
```

La segunda orden usa Docker y dos bases temporales en `127.0.0.1:55447`; aplica Liquibase y ejecuta el backend con `ddl-auto=validate`. No usa los datos habituales. El baseline anterior está fijado por SHA y puede necesitar descargarse con Git; el script admite `BaselineRef` si se necesita otra versión revisada. Sus archivos históricos de prueba no deben publicarse.

`CoordinatorBootstrapIntegrationTest` comprueba login, BCrypt, idempotencia, rechazo de promoción/duplicados, validación, conservación de identidad e invalidación de JWT/recuperación. `CoordinatorBootstrapRunnerTest` comprueba arranque deshabilitado, ausencia de secretos en logs y cierre del contexto en una operación puntual. `CoordinatorBootstrapPostgresIT` comprueba el arranque real del inicializador, login, huella de la credencial retirada e historial de migración con PostgreSQL. Correo real está simulado.

También se ajustaron `BackendApplicationTests.java` para aislar el arranque y `NotificationTransactionTest.java` para adaptar la construcción del servicio de login después de retirar su dependencia de `JdbcTemplate`.

### Resultados locales del 30 de octubre de 2026

| Verificación | Resultado | Evidencia local, desde la carpeta común FaceLit |
|---|---|---|
| Regresión completa del backend, después de aislar la prueba de arranque | 178 pruebas aprobadas, sin fallos ni omisiones | `ci-evidence/hu46-bootstrap-backend-regression-final.log` |
| Integración del inicializador, configuración y regresión de notificaciones | 12 pruebas aprobadas, sin fallos ni omisiones | `ci-evidence/hu46-bootstrap-tests-final.log` |
| PostgreSQL: instalación desde cero | 4 pruebas aprobadas, sin fallos ni omisiones; login correcto con la cuenta de prueba | `ci-evidence/hu46-bootstrap-postgres/fresh-tests.log` |
| PostgreSQL: actualización del baseline anterior y rotación | 4 pruebas aprobadas, sin fallos ni omisiones; UUID y checksum históricos conservados | `ci-evidence/hu46-bootstrap-postgres/upgrade-tests.log` |
| Liquibase en ambos escenarios | `validate` y `update` exitosos | `ci-evidence/hu46-bootstrap-postgres.log` |

Las contraseñas usadas en pruebas son sintéticas y las bases PostgreSQL se eliminan al terminar. Estos resultados verifican la implementación, no una renovación de credenciales de usuarios reales. No publicar el archivo ni la carpeta `baseline` generados por la prueba: contienen la versión histórica del seed.

## Evidencia que QA debe adjuntar y decisión

- [ ] Rama, SHA de entrega y URL del PR de backend y DB. No usar el SHA base como evidencia de cambios sin commit.
- [ ] Log CREATED o ROTATED según el escenario y consulta de identidad/rol, sin contraseña ni hash.
- [ ] Ingreso visual correcto y backend activo tras completar el inicializador.
- [ ] En actualización: mismo UUID/historial, rechazo de contraseña anterior e invalidación de sesiones/recuperaciones.
- [ ] Pruebas automatizadas exitosas y ejecución del script actualizado sin falso éxito ante un error.
- [ ] Si un ambiente utilizó la credencial publicada: evidencia de su renovación en ese ambiente. Una creación nueva local no acredita rotación en Hostinger.
- [ ] CI del SHA entregado y revisión de una persona distinta de quien implementó.

Resultado del revisor: **pendiente**. QA puede aprobar cuando el alcance y la evidencia anterior sean suficientes; puede devolver la entrega indicando caso, rama/SHA, resultado esperado y resultado obtenido. No adjuntar al PR archivos `.env`, contraseñas, JWT, hashes ni el baseline histórico generado por las pruebas.

No se afirma una prueba con cámara, una entrega en producción ni un nuevo puntaje de evaluación: este punto corrige la inicialización del coordinador y su credencial, no valida por sí solo todos los flujos E2E de FaceLit.

## Nota posterior de conectividad facial local

Después del ingreso confirmado por la usuaria, se diagnosticó un conflicto en el puerto 8090 de Windows y se publicó el microservicio local en 8091. La causa, las dos variables privadas modificadas y los pasos para QA se documentan en [HU46_PUERTO_FACIAL_QA.md](HU46_PUERTO_FACIAL_QA.md). Es un ajuste local de conectividad, separado de la inicialización del coordinador; no declara un cambio de puerto en producción ni una aceptación facial E2E.
