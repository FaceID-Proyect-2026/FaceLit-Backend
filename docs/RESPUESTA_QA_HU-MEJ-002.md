# HU-MEJ-002 — Respuesta a observaciones y solicitud de nueva revisión

**Responsable:** MariaOyola.
**Fecha:** 25 de octubre de 2026, hora de Colombia.
**Estado:** correcciones implementadas y pruebas automatizadas aprobadas localmente, entregadas para revisión mediante PR. CI remoto y aprobación independiente pendientes; no declaro la HU Done.

Hola. Revisé las observaciones de recuperación de contraseña y sesiones. Comparto qué ya estaba implementado, qué corregí, los archivos involucrados y las comprobaciones realizadas, para que puedas reproducirlas y decidir si la HU se aprueba o necesita otros ajustes.

## 1. Versiones que deben revisarse

| Repositorio | Rama | SHA base antes de las correcciones actuales | PR de referencia |
|---|---|---|---|
| FaceLit | `hu-37-dev` | `236d90a6887f6ec8a6806068a5700bbfc689b2ba` | [#71](https://github.com/FaceID-Proyect-2026/FaceLit/pull/71) |
| FaceLit-Backend | `hu-38-dev` | `d6d71e2def04579036f10cf6662e4cc8466a09cc` | [#64](https://github.com/FaceID-Proyect-2026/FaceLit-Backend/pull/64) |
| FaceLit-DB | `hu-10-db` | `8d5bf32cdf00909b814a856ce6b4feffea7537f4` | [#59](https://github.com/FaceID-Proyect-2026/FaceLit-DB/pull/59) |

**Los SHA anteriores identifican la base de la corrección, no su commit final.** Revisar el último commit de los PR enlazados y registrar ese SHA junto con el run de CI. Las correcciones se entregan en las mismas ramas de HU-002; verificar también la vigencia de los PR.

La configuración de CI se había preparado después en ramas separadas. Ahora los workflows se incorporaron también a las ramas de HU-002 para que sus PR comprueben la versión que contiene esta historia. No basta con que otro PR de CI esté verde sobre una base distinta.

## 2. Respuesta a cada observación

| Observación | Estado y respuesta | Evidencia que puede revisar QA |
|---|---|---|
| Enviar correo + código y usar resetToken | Ya estaba en la rama base del frontend. Se conserva: verify-token recibe correo/código, devuelve resetToken y reset-password recibe correo/autorización/contraseña. | `app/auth/verify-identity.tsx`, `features/auth/hooks/useNewPasswordForm.ts`, `shared/services/passwordRecoveryService.js`, `shared/services/recoveryAuthorization.ts`. |
| Logout antes de borrar el token y manejo de desconexión | Ya existía la llamada al servidor antes de eliminar el token. Se comprobaron respuesta exitosa, sesión ya inválida y errores de red/servidor. | `shared/services/authService.js`, `shared/contexts/AuthContext.tsx`, pruebas del frontend. |
| Actualizar interfaz ante un 401 | Se completaron las guardas de perfil, configuración y notificaciones. Además se corrigió la restauración, que podía volver a poner al usuario en memoria tras recibir 401. | Tres pantallas, `AuthContext.tsx`, `api.js` y `test-recovery-session.cjs`. |
| Comprobar HTTP 401 por cambio de rol/desactivación | Las pruebas ahora comprueban el 401 mediante MockMvc en una ruta protegida, además de validar la revocación internamente. | `RecoverySessionSecurityIntegrationTest.java`. |
| Dos restablecimientos simultáneos | Se añadió una prueba que inicia dos solicitudes con la misma autorización. Se obtuvo exactamente un 200 y un 400; la autorización queda consumida y la sesión anterior se rechaza. | Prueba `concurrentPasswordResetsConsumeAuthorizationOnlyOnce`. |
| Migración y rollback en PostgreSQL | Se aplicó el baseline de 41 changesets, se aplicó 023, se revirtió y se reaplicó. Se verificó la estructura en cada fase y se retiró el entorno temporal. | `scripts/test-hu002-migration.ps1` y log local del ciclo. |
| Límites de envío | Se añadió una prueba de una solicitud por minuto y tres por ventana de quince minutos. Verifica 429 y Retry-After; las fechas de las ventanas se desplazan en la BD H2 de prueba. | Prueba `recoverySendingLimitsPersistAcrossRequests`. |
| Comportamiento detrás del proxy | La prueba existente comprueba que cambiar X-Forwarded-For no evita el límite del peer remoto. Falta validar la IP efectiva y el proxy confiable del despliegue real. | `RecoveryRateLimitFilter.java`, prueba de IP y aceptación pendiente del despliegue. |
| Documentación, TypeScript, web/Android y CI | TypeScript, lint, scripts y suite Java aprobados localmente. La responsable confirma que el flujo web le permitió iniciar sesión correctamente. Android, CI remoto de estas correcciones y revisión independiente siguen pendientes. | Resultados de abajo y matriz de aceptación. |

## 3. Error observado durante la prueba manual

En la pantalla de verificación apareció `timeout of 120000ms exceeded`. Ese mensaje indicaba que la solicitud no recibió respuesta dentro del tiempo permitido; no demostraba que el código fuera incorrecto.

Durante el diagnóstico, el backend escuchaba en 8080, pero una consulta de diagnóstico también agotó su espera. Sus hilos estaban esperando lecturas del driver PostgreSQL. La consulta de actividad de la base no mostró transacciones bloqueadas. Se reinició únicamente el backend local para renovar conexiones y cargar la versión actual; la base de desarrollo no se reinició ni se borró.

**No se certificó la causa exacta de la espera de infraestructura.** La evidencia apunta a un proceso/conexiones que no respondían; no permite asegurar que fuera Gmail, un código inválido, una consulta bloqueada o un defecto de resetToken. El backend confirmó su arranque después del reinicio y la responsable reportó posteriormente que pudo completar el flujo e iniciar sesión correctamente.

También se identificaron y corrigieron problemas concretos de interfaz:

- Los timeouts podían contarse como intentos locales de código incorrecto. Ahora los fallos de transporte y respuestas 5xx no consumen esos intentos locales; el servidor sigue aplicando sus propios límites.
- Se mostraba el mensaje técnico del error. Ahora se utilizan los mensajes de conexión/servidor existentes en las traducciones.
- Verificar y reenviar podían superponerse. Se añadió una referencia de solicitud en curso compartida por ambas acciones.
- La verificación tiene un timeout de 15 segundos y la solicitud de correo de 30 segundos. Esto limita la espera del cliente; no acelera SMTP, no amplía la vigencia del código y no cancela automáticamente una operación ya iniciada en el servidor.

No se eliminaron contadores, no se desactivó el bloqueo y no se cambió la contraseña directamente mediante SQL.

## 4. Qué significa resetToken

El código de seis dígitos enviado por correo permite verificar la recuperación. Cuando se valida junto con el correo, el backend devuelve una autorización aleatoria larga, llamada `resetToken`. El frontend la guarda solo en memoria y la envía automáticamente para guardar la contraseña nueva. El backend conserva su hash, mantiene el vencimiento de la recuperación y la consume junto con el cambio de contraseña.

No es el JWT de sesión y no es un código adicional que deba escribir la persona.

`resetState` de la pantalla facial pertenece a la solicitud de reemplazo del registro facial. El hook facial también utiliza un parámetro llamado resetToken, pero corresponde a esa autorización facial, no a la recuperación de contraseña. El ajuste de lint de `[resetState?.requestId]` a `[resetState]` no cambia ninguna autorización ni su validación: solo alinea las dependencias del callback con el objeto que lee.

## 5. Archivos de la corrección actual que se deben revisar y publicar

Este inventario enumera la entrega actual; los contratos de recuperación y logout ya existentes se identifican en la sección 2 para revisión, sin afirmar que se reescribieron.

### FaceLit — frontend

| Archivo relativo al repositorio | Cambio y motivo |
|---|---|
| `app/profile/index.tsx` | Usa la guarda existente; evita cargar/mostrar perfil sin sesión y permite redirigir al login. |
| `app/profile/settings.tsx` | Añade guarda y espera la autenticación antes de cargar preferencias. |
| `app/notifications/index.tsx` | Añade guarda y oculta contenido cuando la sesión se rechaza. |
| `shared/contexts/AuthContext.tsx` | Un 401 al restaurar no vuelve a activar el usuario; conserva el respaldo anterior ante un fallo de conexión. |
| `shared/services/api.js` | Espera la eliminación local del token y notifica el rechazo a AuthContext incluso si falla el almacenamiento. Retira un comentario desactualizado. |
| `features/auth/hooks/useVerificationCode.ts` | Separa fallos de conexión/servidor de códigos incorrectos y evita solicitudes superpuestas. |
| `shared/services/passwordRecoveryService.js` | Limita la espera de verificación y solicitud de correo; mantiene los payloads y endpoints. |
| `app/apprentice/facial.tsx` | Incorpora el ajuste de una dependencia de useCallback previamente autorizado para que lint no falle; cuerpo del callback intacto. |
| `scripts/test-recovery-session.cjs` | Nueva prueba repetible de contratos, estado, logout, 401, restauración, redirección y errores de conexión. |
| `scripts/test-entry-platform.cjs` | Adapta la simulación a la llamada real de logout y conserva las verificaciones anteriores. |
| `package.json` | Ejecuta los seis scripts mediante npm test y añade test:security. Sin dependencias nuevas de producción. |
| `.github/workflows/ci.yml` | Incorpora el workflow existente para probar esta rama de HU-002 en PR. |
| `.gitignore` | Excluye salidas generadas de cobertura; no excluye código de pruebas. |
| `docs/HU-MEJ-002-REVISION-QA.md` | Seguimiento técnico, evidencia y aceptación pendiente. |

### FaceLit-Backend

| Archivo relativo al repositorio | Cambio y motivo |
|---|---|
| `src/test/java/com/FaceLit/backend/auth/RecoverySessionSecurityIntegrationTest.java` | Añade/restablece evidencia HTTP 401, concurrencia de restablecimiento y límites de envío. Usa H2 y correo simulado. |
| `.github/workflows/ci.yml` | Incorpora Maven verify y JaCoCo a esta rama para CI de la HU. |
| `.gitignore` | Evita añadir nuevos archivos del caché Maven local. No retira automáticamente los ya versionados. |
| `docs/RESPUESTA_QA_HU-MEJ-002.md` | Respuesta consolidada a QA, inventario de entrega y decisión independiente. |
| `docs/HU-MEJ-002-recuperacion-y-sesiones.md` | Aclara que la ejecución del 24/10 es histórica y que la base ya se había publicado. |
| `docs/HU-MEJ-002-REVISION-QA.md` | Seguimiento de la revisión actual. |

No se modificó Java de producción en esta ronda de correcciones.

### FaceLit-DB

| Archivo relativo al repositorio | Cambio y motivo |
|---|---|
| `scripts/test-hu002-migration.ps1` | Reproduce aplicación, rollback y reaplicación de 023 en PostgreSQL desechable, con protección ante reutilización de un entorno existente y limpieza final. |
| `.github/workflows/ci.yml` | Ejecuta la comprobación de esquema y el ciclo específico de HU-002; conserva los logs como artifact. |
| `.gitignore` | Excluye evidencias temporales .qa-evidence. |
| `docs/HU-MEJ-002-REVISION-QA.md` | Documenta alcance y aceptación pendiente. |

No se cambió contenido de SQL, rollback ni changesets históricos durante esta ronda. La migración 023 y su rollback ya formaban parte de la base de HU-002.

## 6. Pruebas de código frente a archivos temporales

**Sí se versionan:** pruebas Java bajo src/test, scripts de prueba .cjs/.ps1, comandos npm, workflows y documentación. Son necesarios para repetir la verificación y detectar regresiones. Las pruebas Java no forman parte de las clases de producción del JAR; los scripts no son pantallas ni se importan desde la aplicación; los workflows se ejecutan en GitHub Actions.

**No se incluyen como código fuente:** .env, contraseñas/códigos/JWT/resetToken, node_modules, .expo, target, coverage, coverage-v8, .m2, logs completos ni directorios de evidencias/baselines. Los informes necesarios se entregan de forma controlada como artifacts de CI o evidencia revisada, sin secretos.

El backend ya tenía 1.450 archivos del caché .m2 versionados antes de esta corrección. Añadir .m2 a .gitignore impide agregar nuevos archivos no rastreados, pero no elimina los anteriores ni oculta sus cambios. Su retirada del historial/índice es una limpieza separada; no se mezcla con esta HU ni se atribuye a las pruebas nuevas.

También existen eliminaciones locales de otros documentos que no forman parte de esta entrega: docs/mobile-entry-validation.md en frontend y varios documentos previos del backend. No se deben incluir por accidente mediante git add .; publicar únicamente la lista de la sección 5.

**No se creó una rama Git QA ni otra historia de usuario.** facelit-hu002-qa fue el nombre de un proyecto Docker temporal; se retiraron sus contenedores, red y volumen. Los nombres hu002-qa de los logs identifican evidencia de pruebas. No son ramas ni servicios de producción. Las carpetas de evidencia/baseline pueden conservarse localmente y quedan fuera de la entrega de código.

Las pruebas automatizadas no garantizan ausencia total de fallos ni eliminan la posibilidad de conflictos al integrar ramas. Los conflictos dependen de cambios coincidentes; deben resolverse revisando el diff, ejecutando CI y realizando revisión independiente.

## 7. Resultados y límites

| Comprobación | Resultado observado | Alcance real |
|---|---|---|
| npm test | Seis scripts aprobados | Contratos y comportamiento de módulos con adaptadores/hooks simulados; no interacción visual real. |
| TypeScript | Salida 0 | Consistencia de tipos. |
| Lint global | 0 errores, 47 advertencias | No se ocultaron errores para obtener el resultado. |
| Backend completo | 137 pruebas; 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS | Suite local, Java 21 con compilación release 17; runner Java 17 pendiente de observar. |
| JaCoCo backend | 66,60 % de cobertura de líneas | No es cobertura total ni garantía de corrección de todas las rutas. |
| Migración PostgreSQL | 023 aplicada, revertida y reaplicada | Base temporal; verificación estructural, sin operar sobre producción. |
| Web | La responsable confirma que el flujo le funciona y permite iniciar sesión | Reporte manual de la responsable, no revisión independiente de QA. Falta registrar por separado el rechazo de la contraseña anterior si cambió. |
| Android | Pendiente | La responsable dispone de celular; la disponibilidad no equivale a prueba ejecutada. |
| Proxy real | Pendiente | La prueba de cabeceras en MockMvc no sustituye el despliegue. |
| CI remoto de estas correcciones | Pendiente de verificar | Workflows incluidos para que el push al PR ejecute esta versión; registrar resultados y enlaces reales. |

Evidencia local disponible: hu002-qa-frontend-tests.log, hu002-qa-backend-full.log, hu002-qa-lint.log, ci-evidence/hu002/postgres-retry/migration-rollback.log y los reportes Surefire/JaCoCo del backend. No adjuntar logs completos sin revisar su contenido. La salida de backend incluye mensajes de inicialización y SQL, por lo que no se considera material listo para publicación íntegra.

## 8. Comandos de reproducción

Frontend, desde FaceLit:

```powershell
npm test
npm run test:security
npx --no-install tsc --noEmit
npm run lint
```

Backend, desde FaceLit-Backend, con variables ficticias de prueba o el entorno CI preparado:

```powershell
.\mvnw.cmd -B -ntp '-Dspring.profiles.active=test' org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.12:report
```

En la ejecución local se usó el Maven ya instalado con -Dmaven.repo.local=.m2/repository y modo offline. Eso no debe confundirse con el caché que se descarga limpiamente en el runner.

Base de datos, desde FaceLit-DB, con Docker disponible y el SHA baseline presente en el historial:

```powershell
.\scripts\test-hu002-migration.ps1
```

El script utiliza el proyecto facelit-hu002-qa y puerto temporal 55440; rechaza reutilizar un proyecto existente con ese nombre. No utilizar comandos de reset ni borrar volúmenes de la base habitual para reproducirlo.

## 9. Revisión solicitada a QA

- [ ] Confirmar los SHA finales de los tres repositorios después de publicar y revisar sus diffs.
- [ ] Verificar que solo se incluyeron los archivos autorizados, sin cachés, secretos ni eliminaciones de documentos ajenos.
- [ ] Repetir recuperación, login con contraseña nueva y rechazo de la anterior si es diferente.
- [ ] Probar logout, desconexión y 401 desde perfil, configuración y notificaciones.
- [ ] Revisar los casos MockMvc de revocación, concurrencia y límites.
- [ ] Revisar logs del ciclo PostgreSQL y reproducirlo si corresponde. La concurrencia del restablecimiento se probó en H2, no en PostgreSQL real.
- [ ] Completar aceptación en Android y en el proxy del entorno de despliegue.
- [ ] Adjuntar enlaces de CI verde de esta versión y registrar revisión independiente.

**Decisión de QA:** [ ] Aprobada [ ] Aprobada con observaciones [ ] Requiere correcciones.
**Revisora:** ____________________.
**Fecha y SHA revisados:** ____________________.
**Observaciones/evidencia:** ____________________.

Solicito que la decisión se tome sobre los cambios y evidencias indicados, manteniendo separados los resultados comprobados de las validaciones pendientes.
