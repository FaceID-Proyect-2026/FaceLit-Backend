# HU-MEJ-002 — Seguimiento de correcciones y aceptación QA

Estado: correcciones y validación automatizada entregadas para revisión por PR. No declarar Done; falta completar aceptación web/Android, proxy del despliegue, CI remoto y revisión independiente.

## Versiones identificadas

| Repositorio | Rama de trabajo | SHA base antes de estas correcciones |
|---|---|---|
| FaceLit | hu-37-dev | 236d90a6887f6ec8a6806068a5700bbfc689b2ba |
| FaceLit-Backend | hu-38-dev | d6d71e2def04579036f10cf6662e4cc8466a09cc |
| FaceLit-DB | hu-10-db | 8d5bf32cdf00909b814a856ce6b4feffea7537f4 |

Estas referencias identifican la base. Para reproducir la corrección publicada, utilizar el HEAD del PR correspondiente y registrar su SHA y run de CI.

## Lo que ya existía y se conserva

La verificación envía correo y código; el resetToken recibido se conserva únicamente en memoria, vinculado al correo, y se usa para el restablecimiento. Logout llama al servidor antes de limpiar el token. No se pide otro token al usuario, no se registra la autorización en una URL ni se almacena persistentemente.

## Correcciones de frontend

- Perfil, configuración y notificaciones usan la guarda de autenticación existente: ocultan contenido sin sesión y redirigen al login cuando el router está listo.
- La restauración de sesión no vuelve a colocar el usuario en memoria cuando /api/profile/me responde 401. Se conserva el fallback anterior frente a un fallo de conexión.
- El interceptor espera la limpieza de almacenamiento y notifica a AuthContext incluso si falla esa limpieza. Se retiró el comentario desactualizado que afirmaba que esa notificación no existía.
- Los fallos de transporte y de servidor al verificar un código no consumen los intentos locales. El bloqueo 429 del backend sigue vigente. Se impide superponer reenvío y verificación; se usan mensajes de conexión existentes en las traducciones.
- Verificación: timeout de 15 segundos. Solicitud de correo: 30 segundos. Esto limita la espera del cliente, no garantiza entrega de correo ni cancela automáticamente un procesamiento que ya haya empezado en el servidor.
- Se incorpora a esta rama el ajuste de lint previamente autorizado en la pantalla facial: [resetState?.requestId] pasa a [resetState]. No cambia validación facial ni el resetToken de recuperación de contraseñas.

## Pruebas de regresión

scripts/test-recovery-session.cjs comprueba contratos de recuperación, aislamiento por correo de la autorización en memoria, logout servidor-primero, conservación del token ante error de conexión, manejo del 401, restauración rechazada y redirección con el router listo. También comprueba que los fallos de conexión no agoten intentos y que no se solapen verificación/reenvío.

Se ejecuta junto con los cinco scripts existentes mediante npm test; npm run test:security permite ejecutarlo por separado. La simulación de logout del script de entrada se adaptó al endpoint real, conservando sus aserciones de almacenamiento en web/Android/iOS.

Estos scripts transpilan módulos y simulan adaptadores/hooks: no son pruebas de interacción visual, no ejecutan React Native en un teléfono y no prueban entrega SMTP real.

## Resultados locales observados

- npm test: seis scripts aprobados, incluidas 168 combinaciones de notificaciones/idioma y 13 casos de acceso facial.
- TypeScript final: salida 0. Lint global: salida 0, sin errores y con 47 advertencias existentes.
- Backend, suite completa con JaCoCo: 137 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS. Cobertura de líneas: 66,60 %. Ejecución local con JDK 21 y compilación release 17; Java 17 del runner aún debe verificarse en GitHub.
- RecoverySessionSecurityIntegrationTest incluye HTTP 401 mediante MockMvc tras cambio de rol/desactivación, dos reset-password simultáneos con una sola autorización (un 200 y un 400), y límites de envío de una solicitud por minuto/tres por ventana de quince minutos. El tiempo de las ventanas se simula moviendo sus fechas en H2; no se espera quince minutos reales.
- PostgreSQL temporal: baseline de 41 changesets, aplicación de 023 (42 en total), rollback a la etiqueta anterior y reaplicación de 023 aprobados. Se verificaron la presencia/ausencia de auth_rate_limit y de las tres columnas nuevas. No se alteraron migraciones históricas ni la BD de desarrollo.
- El proyecto Docker facelit-hu002-qa y su volumen temporal se retiraron al terminar. La prueba se reproduce desde FaceLit-DB/scripts/test-hu002-migration.ps1.

Evidencia local fuera del código fuente: ../hu002-qa-frontend-tests.log, ../hu002-qa-backend-full.log, ../ci-evidence/hu002/postgres-retry/migration-rollback.log y FaceLit-Backend/target/surefire-reports. No subir cachés, logs con datos personales ni target como código.

## CI de la versión correcta

Se incorporan los workflows existentes de HU-MEJ-006 a las ramas actuales de HU-MEJ-002 para que su PR ejecute los cambios de esta HU. El workflow de BD también ejecuta el script de rollback/reaplicación contra un proyecto temporal distinto del PostgreSQL principal de su job.

Esto no es evidencia de ejecución verde en GitHub: hasta observar el run de estos cambios y adjuntar su enlace, CI queda pendiente. Tampoco configura ni elude las protecciones de ramas.

## Aceptación manual: realizar en web y repetir en Android

Usar una cuenta de prueba autorizada; introducir contraseñas/códigos únicamente en la aplicación. No adjuntar códigos, resetToken ni JWT completos a capturas o logs.

| Caso | Acción | Resultado esperado | Resultado real |
|---|---|---|---|
| QA-002-01 | Solicitar un código y verificar el último recibido | Se abre nueva contraseña sin pedir una autorización adicional | Flujo web reportado correcto por la responsable; QA independiente y Android pendientes |
| QA-002-02 | Guardar una contraseña distinta e iniciar sesión | Nueva aceptada; anterior rechazada | Inicio de sesión correcto reportado en web; rechazo de la anterior y Android no confirmados |
| QA-002-03 | Recargar la pantalla de nueva contraseña antes de guardar | No permite restablecer sin autorización en memoria; informa reiniciar recuperación | Pendiente |
| QA-002-04 | Cerrar sesión, volver atrás y abrir perfil/configuración/notificaciones | No muestra contenido privado; redirige al login | Pendiente |
| QA-002-05 | Provocar rechazo 401 de una sesión de prueba desde perfil/configuración/notificaciones | Limpia estado y redirige al login | Pendiente visual; módulos simulados aprobados |
| QA-002-06 | Desconectar la red al verificar o cerrar sesión | Error legible; no declara éxito ni agota intentos por conexión | Pendiente visual; módulos simulados aprobados |
| QA-002-07 | Cinco códigos erróneos y recarga/reinicio de app | Quinto 429; no se evita el bloqueo recargando | Evidencia parcial aportada por usuaria; registrar HTTP y versión |

La prueba de concurrencia automatizada usa H2; el ciclo Liquibase usa PostgreSQL. No se presentan como prueba de concurrencia del restablecimiento contra PostgreSQL real ni como aceptación del despliegue.

## Proxy y cierre

El filtro utiliza getRemoteAddr y rechaza la evasión mediante X-Forwarded-For arbitrario en las pruebas existentes. En el despliegue real, identificar proxy confiable e IP efectiva, comprobar dos clientes y verificar Retry-After. No habilitar confianza indiscriminada en cabeceras ni declarar el proxy validado con localhost.

Adjuntar SHA final, PR, enlace a cada CI verde, resultados manuales por plataforma y aprobación de una persona distinta de la autora. Los PR de referencia antes de actualizar: frontend #71, backend #64 y BD #59 de FaceID-Proyect-2026. Verificar su vigencia al publicar.
