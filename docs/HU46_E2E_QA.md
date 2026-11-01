# HU46 — Recuperación y revisión del ejecutor E2E

Fecha de revisión: 31 de octubre de 2026. Punto 3 del plan de mejora del 30 de octubre; hallazgo H-02, también referido como H-20 en ese informe.

## Alcance y preservación de lo implementado

Se recupera únicamente `scripts/test-architecture-e2e.ps1` del repositorio FaceLit-DB, presente en `origin/hu-44-dev` y originado en `d80a9ba`. No se cambia a HU44, no se fusiona esa rama ni se reemplazan carpetas, migraciones o módulos actuales. La prueba ejecuta los archivos actuales de las ramas HU46, incluidos los arreglos de sesiones, inicialización segura del coordinador y procesamiento facial. Las dos APIs conservan PostgreSQL compartida y sus capas actuales.

| Repositorio | Rama de trabajo | SHA de partida |
|---|---|---|
| FaceLit | hu-46-dev | c6e8cf606d883e8c272f07f712a904f8daf19f20 |
| FaceLit-Backend | hu-46-dev | c5970522984c014d5a30146c5e0a5c03d6e41b43 |
| FaceLit-DB | hu-46-main | a5e5f9e27c6d57ee63a224bfa5c5f90dbec376f8 |
| micro-facial | hu-46-dev | 4a94d097d7bd60625c5543a879c8769c2b3df2e2 |

Estos son SHA de partida, no contienen las modificaciones documentales y del ejecutor aún pendientes de commit. Cada ejecución registra su rama, SHA y archivos con cambios en `revisions.json`.

## Archivos de esta corrección

- FaceLit-DB `scripts/test-architecture-e2e.ps1`: recuperación del ejecutor, comprobación de dependencias, selección opcional de Maven, registro de revisiones y evidencia de prueba, desactivación de bootstrap durante este caso y comprobación de que el test no se omitió. Espera de arranque ampliada mediante un override temporal de Compose y conservación del log PostgreSQL antes de limpiar recursos.
- FaceLit-DB `README.md`: comando, requisitos, aislamiento y límites de la prueba.
- FaceLit-DB `scripts/tests/test-native-command-logging.ps1`: regresión del manejo de stderr y códigos de salida en Windows PowerShell 5.1 y PowerShell 7, sin Docker ni base de datos.
- FaceLit-Backend `docs/HU46_E2E_QA.md`: este registro de revisión.
- FaceLit-Backend `README.md`: enlace a esta revisión y límites de aceptación.

Las actualizaciones previas de `HU46_SESIONES_AMBIENTES_QA.md` y `HU46_COORDINADOR_QA.md` se conservan. No se modifica lógica de producción ni la seguridad biométrica para conseguir una prueba verde.

## Qué comprueba el ejecutor existente

Aplica Liquibase a una base nueva y valida las entidades Hibernate. Levanta FastAPI con la API, servicios y repositorios actuales; sustituye solamente la inferencia por señales deterministas mediante `tests/support/e2e_app.py`. El backend usa HTTP real, JWT y SQL; el envío de correo está simulado.

`ArchitectureFlowPostgresIT` comprueba login de los tres roles, acciones de vida correctas/incorrectas/retiradas, registro de rostro y persistencia, ocultamiento del vector en la respuesta Java, confirmación idempotente y notificación, consulta del estado de registro, rechazo de asistencia por un aprendiz, asistencia del instructor, rechazo de duplicados, reporte y carga CSV.

Limitaciones: configura datos iniciales y la sesión mediante fixtures SQL; no prueba por interfaz toda la creación de sesión. Esa creación y sus permisos tienen sus pruebas específicas descritas en `HU46_SESIONES_AMBIENTES_QA.md`. No cubre salida facial completa, cámara real, entrega SMTP, APK/Android, producción ni latencia de inferencia. La opción de navegador es adicional y no se considera ejecutada salvo evidencia explícita.

El inicializador del coordinador permanece en el código actual, pero sus operaciones Create/Rotate se desactivan para este fixture. Sus resultados y pruebas específicas están en `HU46_COORDINADOR_QA.md`; el login del coordinador sintético de este E2E no sustituye esos casos.

## Ejecución para QA

Requisitos: cuatro repositorios hermanos en las ramas revisadas, Docker Desktop con motor Linux activo y Maven/Java compatibles. No hace falta detener el frontend, backend o microservicio habitual: la prueba usa otros puertos. Los puertos de prueba deben estar libres.

Desde la carpeta FaceLit-DB, en PowerShell:

```powershell
.\scripts\test-architecture-e2e.ps1
```

El comando construye la imagen del micro actual. Si Maven Wrapper no está disponible, puede indicarse un ejecutable Maven instalado con `-MavenCommand 'C:\ruta\a\mvn.cmd'`. Para reutilizar conscientemente una imagen compatible ya instalada:

```powershell
.\scripts\test-architecture-e2e.ps1 -SkipImageBuild -MicroImage 'facelit-embedding-service-embedding-service:latest'
```

El código de `app/` y el fixture se montan desde el checkout actual incluso al reutilizar imagen. Esta modalidad no verifica que una reconstrucción de dependencias resulte exitosa.

Se usan PostgreSQL `127.0.0.1:55439`, FastAPI `127.0.0.1:18090`, Spring `127.0.0.1:18080` y, solo con `-Browser`, web `127.0.0.1:18081`. Las credenciales son sintéticas y exclusivas de la prueba. No se leen las credenciales privadas del micro para acceder al sistema habitual. No se toca la base de desarrollo `5439` ni el servicio facial habitual `8091`.

Resultado esperado: Liquibase validate/update exitosos, prueba HTTP ejecutada sin fallos ni omisiones y mensaje `Integration passed`. Se conservan logs bajo FaceLit-DB `.tmp/facelit-arch-e2e-<id>/`, ignorados por Git; se eliminan únicamente los recursos Docker de ese proyecto temporal. No adjuntar JWT ni volcados de datos privados a QA.

## Registro de ejecución

- Primer intento, proyecto `facelit-arch-e2e-7b23f98a`: PostgreSQL temporal quedó unhealthy durante el arranque; no llegó a ejecutar Liquibase ni Maven. El cleanup retiró únicamente ese proyecto. No se capturó su log PostgreSQL antes de eliminarlo, por lo que no se atribuye una causa interna sin evidencia.
- Se corrige la conservación del log y se amplía exclusivamente la tolerancia de arranque del entorno de prueba. La ejecución posterior se registra al terminar.
- Segundo intento, proyecto `facelit-arch-e2e-472fa5fe`: Liquibase `validate` correcto y 44 changesets aplicados. FastAPI no alcanzó a responder dentro del bucle anterior; no llegó a Maven. El log facial capturado estaba vacío. El bucle antiguo podía agotar sus 40 intentos en aproximadamente 10 segundos si las conexiones se rechazaban inmediatamente. Se reemplaza por una espera medida con reloj, con límite configurable `-StartupTimeoutSeconds` (300 segundos por defecto). No se cambia el timeout de las peticiones del aplicativo ni se atribuye una causa interna a FastAPI sin evidencia adicional.
- Ejecución final, proyecto `facelit-arch-e2e-11476fdd`: **aprobada**, finalizada el 31 de octubre de 2026 a las 14:11:23, hora de Bogotá. Liquibase validó y aplicó los 44 changesets; FastAPI respondió a salud; Hibernate validó el esquema; `ArchitectureFlowPostgresIT` ejecutó **1 escenario integral, 0 fallos, 0 errores, 0 omisiones**. El ejecutor comprobó el XML de resultados y terminó con código 0. El escenario contiene las comprobaciones HTTP y SQL enumeradas arriba; no son una sola petición ni una suite completa de regresión.

Se reutilizó `facelit-embedding-service-embedding-service:latest` con `-SkipImageBuild` y se montaron la aplicación y el fixture actuales. Se usó Maven 3.9.14 instalado localmente y JDK 21, compatible con el objetivo Java 17 del proyecto. No se verificó reconstrucción de la imagen ni ejecución con JDK 17 en este intento. El tiempo Maven fue 3:07 minutos, incluyendo arranque de Spring y pruebas; **no es una medida del tiempo de reconocimiento con cámara**.

Evidencias locales, desde FaceLit-DB: `.tmp/facelit-arch-e2e-11476fdd/revisions.json`, `liquibase-validate.log`, `liquibase-update.log`, `http-e2e.log`, `architecture-tests.xml`, `facial.log` y `postgres.log`. Los contenedores, red y volumen del proyecto temporal fueron retirados al terminar. Estas evidencias corresponden a los SHA de partida más los cambios de trabajo registrados, no a un nuevo commit publicado.

Sintaxis PowerShell y `git diff --check`: aprobados. No se ejecutaron navegador, cámara, pruebas Android ni workflows de GitHub Actions en esta revisión.

### Compatibilidad de terminal — seguimiento del 31 de octubre

La usuaria ejecutó el proyecto temporal `facelit-arch-e2e-0a86ecb1` desde Windows PowerShell y obtuvo `NativeCommandError` al capturar el mensaje informativo de Compose `Container ... Running`. PostgreSQL había quedado healthy; el script se interrumpió por la interpretación de stderr como error bajo `$ErrorActionPreference = 'Stop'`, no por ese mensaje como prueba de una falla de la base. El cleanup retiró el proyecto temporal.

Se centraliza la captura de salida nativa en `Invoke-NativeLogged`: conserva stdout/stderr como texto y decide el resultado mediante el código de salida. Se aplica a Liquibase, Maven y recopilación de logs. La preferencia se cambia solamente dentro de esa función y se restaura; un código distinto de cero continúa fallando en los pasos de prueba. Los logs de cleanup permiten conservar el diagnóstico aunque su lectura falle. No se cambia lógica funcional, migraciones ni configuración del aplicativo.

Validación específica: `scripts/tests/test-native-command-logging.ps1` aprobado en Windows PowerShell **5.1.26100.9711** y PowerShell **7.6.5**. Comprueba conservación de ambas salidas, aceptación de código 0, rechazo de código 7 y conservación de la preferencia del llamador. El E2E integral aprobado anteriormente no se repitió por este ajuste de terminal; QA debe repetir el mismo comando para comprobar su ejecución completa en su consola.

## Integración y pendientes

PR de DB propuesto: **Secure coordinator initialization and restore cross-repository verification**, desde `hu-46-main` hacia `main`. La rama incluye la retirada previa del seed fijo del coordinador y ahora la recuperación del ejecutor de HU44 sobre HU46, sin reemplazar los arreglos recientes. Validación de este incremento: 44 changesets aplicados y escenario HTTP integral aprobado sin omisiones. Las pruebas específicas del coordinador conservan sus evidencias anteriores y deben revisarse por separado. Requiere revisión y checks; no fusionar automáticamente. La documentación del backend debe publicarse mediante su propio PR de HU46 a dev, cuyo alcance también incluye los cambios previos de sesiones y coordinador.

Preparación local para el PR: revisar el diff y las evidencias, hacer commit en inglés en las ramas HU46, publicar esas ramas y abrir/actualizar el PR correspondiente. **Esta revisión no ha hecho commit, push, abierto PR ni fusionado ramas.**

No se eliminan ramas acumuladas: primero deben compararse y clasificarse como absorbidas, pendientes u obsoletas con revisión del equipo. La recuperación local no equivale a integración en main ni aprobación independiente.

El siguiente paso separado es el punto 4: ejecutar las pruebas PostgreSQL y de migración en CI, conservar sus artefactos y demostrar que una prueba rota bloquea el PR. Los workflows actuales revisados aún no activan esos escenarios del backend y microservicio; no se declara resuelto ese punto con una ejecución local.
