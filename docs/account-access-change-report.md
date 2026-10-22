# Registro, cambio de correo y recuperacion de acceso

## Correccion vigente: credenciales generadas por el sistema

La aclaracion del usuario sustituye las propuestas de enlaces y correo pendiente descritas mas abajo. Esas secciones se conservan como registro de la implementacion anterior, no como comportamiento vigente.

- Primer registro manual o CSV: se envia la plantilla original con nombre, rol traducido, documento para iniciar sesion, correo, ficha/programa cuando corresponden y contrasena generada.
- Cambio administrativo de correo, manual o CSV: se guarda la nueva direccion en la misma credencial; el mismo diseno de correo entrega otra contrasena generada e informa que reemplaza la anterior. Las notificaciones y recuperaciones siguientes usan inmediatamente la nueva direccion.
- El usuario puede iniciar sesion directamente con documento y contrasena recibida. Cambiarla es opcional desde `/auth/password-recovery`: correo vigente, codigo de seis digitos y nueva contrasena. No se generan enlaces de configuracion de 24 horas para estos envios.
- Cambiar otros datos o reimportar sin cambios no regenera la contrasena ni duplica un envio exitoso.
- El envio queda registrado para reintentos, hasta tres intentos automaticos. Un fallo SMTP conserva la contrasena anterior; se puede recuperar al correo actualizado o reintentar. La nueva contrasena se activa al aceptar SMTP el envio. Esto no confirma la entrega en la bandeja de entrada.
- La contrasena inicial viaja solo en el evento en memoria posterior al commit y se conserva como hash en la credencial. No se almacena en texto plano en la cola ni en las notificaciones de la app. Un reintento tras fallo/reinicio genera otra contrasena y activa el hash correspondiente al correo enviado; las respuestas administrativas anteriores pueden contener la contrasena inicial sustituida por ese reintento.
- Una recuperacion completada cancela cualquier envio pendiente de credenciales, para que no sobrescriba la contrasena elegida por el usuario. Cambiar nuevamente el correo cancela el envio al destino anterior e invalida sus codigos, conservando las filas historicas.
- Se mantienen los roles internos, permisos, identificador, asignaciones e historial; se reutilizan los servicios existentes. No se agregan migraciones ni se ejecutan cambios en la base de datos por esta correccion.

Archivos ajustados: `AccountAccessService`, `InitialCredentialServiceImpl`, `InitialCredentialsEmailRequested`, `InitialCredentialEmailListener`, `InitialCredentialEmailTemplate`, `PasswordRecoveryServiceImpl` y texto alternativo de `EmailServiceImpl`. Se retira el uso del correo pendiente en las nuevas recuperaciones porque ahora el cambio administrativo es inmediato. Los endpoints y filas anteriores se conservan por compatibilidad.

Las vistas previas de prueba usan datos ficticios: `target/welcome-email-preview.html` y `target/updated-email-preview.html`.

Validacion: suite completa de 107 pruebas sin fallos (`target/generated-credential-tests.log`); tras los ajustes finales, 25 pruebas enfocadas sin fallos (`target/generated-credential-final-tests.log`: 18 de integracion y 7 de plantilla/evento/MIME). Se comprueba registro manual de aprendiz, instructor y coordinador; CSV de aprendiz e instructor y sus cambios de correo; hash correspondiente a la contrasena enviada; reimportacion sin cambios; recuperacion al correo nuevo; invalidacion de codigos anteriores; errores, reintentos y preservacion del historial. H2 y correo simulado, sin datos ni envios reales.

Los botones utilizan `facelit.access-url` / `FACELIT_ACCESS_URL`. En el servidor esa variable debe indicar la direccion publica del frontend; el valor predeterminado sigue siendo el de desarrollo local.

## Registro anterior (sustituido por la correccion vigente)

## Diagnostico comprobado

- `InitialCredentialEmailListener` enviaba la contrasena inicial despues del commit. Ante un fallo solo escribia un log; no existia un envio persistente pendiente.
- `CsvAcademicServiceImpl.findOrCreateUser` no reenviaba instrucciones para cuentas que ya tenian credenciales.
- Ademas, `processApprentice` no llamaba a ese metodo cuando el aprendiz ya existia: omitia sus cambios personales, incluido el correo.
- Los formularios de aprendices, instructores y coordinadores reemplazaban directamente el correo de `security.credential`, sin verificar la nueva direccion.
- La recuperacion por codigo ya existia. Se conserva y se integra con los enlaces nuevos.

No se ha determinado la causa del fallo SMTP original del entorno: las pruebas usan correo simulado.

## Comportamiento implementado

1. El registro conserva la generacion inicial y el hash existentes. Las respuestas administrativas conservan sus contratos de credenciales iniciales.
2. La bienvenida guarda una solicitud durable dentro de la transaccion de registro. Despues del commit se envia un enlace de un solo uso, valido durante 24 horas; la contrasena no se envia por correo.
3. La solicitud conserva solo el SHA-256 del token, su destino, vencimiento, estado, intentos y clase del error. No almacena el enlace ni excepciones que puedan contener secretos.
4. Se reintentan envios pendientes o fallidos cada minuto, hasta tres intentos. Reimportar el CSV puede reactivar la bienvenida fallida o vencida sin crear otra cuenta. No se repite una bienvenida enviada y vigente.
5. Un cambio de correo conserva la direccion vigente y la contrasena actual. El aviso identifica expresamente la direccion anterior y la nueva y dirige a `/auth/password-recovery`. El usuario debe ingresar la nueva direccion, recibir un codigo de seis digitos, verificarlo y establecer su nueva contrasena. Solo entonces se activa el correo nuevo.
6. Una correccion posterior de la direccion pendiente invalida el enlace anterior. Completar el proceso consume las solicitudes anteriores y los codigos de recuperacion del usuario.
7. La recuperacion normal sigue enviando un codigo al correo vigente. Si se introduce una direccion nueva pendiente, el codigo se envia a esa nueva direccion y queda vinculado a la solicitud exacta de cambio. No se envia otro enlace para omitir la verificacion.
8. Una recuperacion de contrasena tambien cancela una bienvenida pendiente, para que un enlace de bienvenida antiguo no cambie posteriormente la contrasena.
9. Los avisos de cambios personales de estos tres roles usan el envio de notificaciones por correo que ya existia. Si no hay cambios, no se crea otro aviso personal.
10. El identificador del usuario y su historial se conservan. Los envios normales consultan el correo vigente al despachar.

## Archivos y motivos

Rutas relativas a `FaceLit-Backend/src/main/java/com/FaceLit/backend/`:

| Archivo | Motivo |
|---|---|
| `auth/model/security/AccountAccessRequest.java` (nuevo) | Persistir solicitudes, verificacion y resultados de entrega. |
| `auth/repository/security/AccountAccessRequestRepository.java` (nuevo) | Consultar pendientes y bloquear solicitudes durante su uso. |
| `auth/service/serviceImpl/security/AccountAccessService.java` (nuevo) | Generar enlaces, enviar, reintentar, verificar y completar sobre la misma cuenta. |
| `auth/dto/request/security/CompleteAccountAccessDTO.java` (nuevo) | Validar el token de enlace y la nueva contrasena sin cambiar el contrato del codigo de seis digitos. |
| `auth/repository/security/CredentialRepository.java` | Serializar la confirmacion de correo y la recuperacion sobre la credencial. |
| `auth/service/security/InitialCredentialService.java` | Exponer cambio de correo y reintento a los servicios existentes. |
| `auth/service/serviceImpl/security/InitialCredentialServiceImpl.java` | Conservar generacion/hash y delegar las instrucciones recuperables. |
| `auth/service/security/InitialCredentialsEmailRequested.java` | Transportar solo el ID de solicitud, sin contrasena. |
| `auth/service/serviceImpl/security/InitialCredentialEmailListener.java` | Sustituir el envio no recuperable y su plantilla de contrasena por el despacho durable. |
| `auth/controller/security/PasswordRecoveryController.java` | Conservar `POST /api/auth/complete-access` para bienvenida y utilizar la respuesta normal de codigo al recuperar un correo pendiente. |
| `auth/service/serviceImpl/security/PasswordRecoveryServiceImpl.java` | Usar el correo vigente sin distinguir mayusculas, coordinar concurrencia e invalidar bienvenidas antiguas. |
| `auth/service/serviceImpl/security/ChangePasswordServiceImpl.java` | Cancelar bienvenidas antiguas cuando el usuario cambia su contrasena. |
| `auth/service/serviceImpl/security/UserManagementServiceImpl.java` | Solicitar verificacion del cambio manual de correo. |
| `academic/service/serviceImpl/academic/InstructorServiceImpl.java` | Solicitar verificacion del cambio manual de correo. |
| `academic/service/serviceImpl/academic/UserChipServiceImpl.java` | Solicitar verificacion y notificar solo datos efectivamente activos. |
| `academic/service/serviceImpl/academic/CsvAcademicServiceImpl.java` | Actualizar tambien aprendices existentes, solicitar verificacion y reintentar bienvenidas pendientes. |
| `notification/service/NotificationCatalog.java` | Habilitar correo en los tres avisos personales ya existentes. |

Otros archivos:

- `FaceLit-DB/01_ddl/04_alter/019_account_access_requests.sql`: nueva tabla aditiva `security.account_access_request`, indices y unicidad de solicitud pendiente por usuario.
- `FaceLit-DB/01_ddl/04_alter/0000changelog.yaml`: registrar la migracion nueva sin editar changesets anteriores.
- `FaceLit/shared/services/passwordRecoveryService.js`: llamar al endpoint de finalizacion de enlace.
- `FaceLit/features/auth/hooks/useNewPasswordForm.ts`: aceptar el enlace ademas del codigo actual.
- `FaceLit/features/auth/hooks/usePasswordRecoveryForm.ts`: avanzar al recorrido obligatorio de verificacion del codigo enviado al correo ingresado.
- `FaceLit/app/auth/new-password.tsx`: ofrecer nuevas instrucciones cuando falla el enlace.
- `FaceLit/app/auth/password-recovery.tsx`: explicar la entrega por enlace.
- `FaceLit-Backend/src/test/java/com/FaceLit/backend/auth/AccountAccessIntegrationTest.java`: escenarios nuevos sobre H2 y correo simulado.

La plantilla que mostraba la contrasena se sustituyo porque el requisito aprobado prohibe enviarla por correo. No se eliminaron usuarios, tablas, historiales ni validaciones de autenticacion.

## Despliegue y limites

- La migracion esta preparada, **no ejecutada**. Debe aplicarse mediante el procedimiento habitual antes de iniciar este backend, que valida el esquema.
- `facelit.access-url` debe apuntar a la direccion real de la aplicacion para que los enlaces abran la pantalla existente.
- No se hicieron despliegues, llamadas a SMTP real ni modificaciones a datos reales.
- H2 no valida los indices parciales de PostgreSQL de esta migracion. Su ejecucion sobre PostgreSQL y la entrega real deben comprobarse en un entorno de prueba aprobado.
- Las cuentas antiguas sin registros de entrega pueden usar la recuperacion existente. No se supone que nunca recibieron un correo ni se envian bienvenidas masivas a todas ellas.
- Se conservan los tipos originales de CSV; no se agrega importacion de coordinadores. Coordinadores usan su formulario existente y comparten el mismo mecanismo de verificacion.

## Validacion

- **Backend: 94 pruebas, 0 fallos, 0 errores, 0 omitidas.** Ejecucion completa terminada el 21 de octubre de 2026, con `BUILD SUCCESS`. Se mantienen intactas las pruebas anteriores.
- **15 pruebas nuevas** en `AccountAccessIntegrationTest`: bienvenida por enlace sin contrasena, hash de token, uso unico, fallos de correo persistentes, reintento sin duplicados, conservacion de contrasena sin cambios, activacion del correo verificado, invalidacion de codigos anteriores, reenvio al destino pendiente, vencimiento, correcciones posteriores, limite de tres intentos, recuperacion normal, registro/cambio manual en los tres roles, reimportacion CSV, avisos de datos personales y destinatario de futuras notificaciones con historial conservado. El caso parametrizado de los tres roles cuenta como tres ejecuciones.
- **Frontend: `tsc --noEmit --pretty false` termino con codigo 0.** No se realizo prueba visual o prueba de navegacion en un navegador/dispositivo.
- `git diff --check` no detecto errores de espacios en los archivos revisados.
- Evidencia de backend: `FaceLit-Backend/target/account-access-tests.log` y los XML/TXT en `FaceLit-Backend/target/surefire-reports/`.

La ejecucion uso Maven 3.9.14 ya disponible localmente, en modo offline, con estos argumentos:

```text
-o test -Dmaven.repo.local=.m2/repository -Dspring.profiles.active=test -Dspring.mail.host=127.0.0.1 -Dspring.mail.port=1 -Dlogging.level.org.hibernate.SQL=OFF
```

Las primeras pruebas nuevas detectaron y permitieron corregir la omision de actualizacion de aprendices existentes en CSV. La prueba de despacho se ejecuta sincronicamente dentro de su transaccion de prueba para verificar el correo vigente sin depender de un listener asincrono que solo ve datos despues del commit.

Pendientes de entorno: aplicar y validar la migracion en PostgreSQL; verificar una entrega real con el proveedor configurado y abrir el enlace en la URL publica de la aplicacion. No se afirma que esas verificaciones ya se hayan ejecutado.

## Correcciones de textos, detalle de cambios y campo de contrasena

Cambios adicionales aprobados por el usuario el 21 de octubre de 2026:

- `AccountAccessService.java`: consulta el rol ya asignado e incluye Aprendiz, Instructor o Coordinador en el correo. Conserva el enlace y no vuelve a enviar contrasenas.
- `notification/service/ProfileChangeNotificationService.java` (nuevo): compara una lista permitida de campos personales y prepara `profileChanges`, responsable obtenido de la sesion y fecha del evento. Solo envia las copias al usuario afectado y al coordinador responsable identificado, sin duplicar destinatarios ni difundir estos valores a otros coordinadores. No admite campos de contrasena o token.
- `UserChipServiceImpl.java`, `InstructorServiceImpl.java`, `UserManagementServiceImpl.java` y `CsvAcademicServiceImpl.java`: reutilizan ese servicio en los avisos de perfil. Se conservan los registros de historial y los eventos ajenos a datos personales. Los cambios simultaneos de nombre y tipo de instructor se resumen en un unico aviso de perfil.
- `features/notifications/types.ts`, `localizeNotification.ts` y `app/notifications/index.tsx`: muestran campos, valores anteriores/nuevos, responsable y fecha en America/Bogota. Las notificaciones antiguas sin estos metadatos conservan su texto disponible; no se actualiza su registro en la base de datos.
- `shared/i18n/notificationLifecycleTemplates.ts`: etiquetas de los detalles en los cuatro idiomas existentes, incluidos roles, estados y tipos de instructor comprensibles.
- `app/auth/login.tsx`: input reducible con `minWidth: 0` y boton del ojo de ancho reservado, sin modificar el estado de la contrasena ni el envio de credenciales. Incluye etiqueta accesible para mostrar/ocultar.
- `ProfileChangeNotificationTest.java`, `AccountAccessIntegrationTest.java` y `scripts/test-notification-i18n.cjs`: verifican diferencias reales, destinatarios, exclusion de secretos, textos del correo y renderizado traducido.

Estas correcciones no agregan migraciones, dependencias ni cambios de permisos. La sustitucion de los avisos genericos y de las copias generales de perfil permite mostrar el mismo detalle al afectado y al responsable, evitando duplicados; los otros tipos de notificacion conservan su funcionamiento.

La comprobacion visual del login en navegador/dispositivo queda pendiente: la herramienta de navegador no encontro navegadores conectados. No se realizaron inicios de sesion reales ni pruebas de correo con destinatarios reales.

## Cambio de correo con codigo obligatorio

La indicacion posterior del usuario sustituye el enlace directo de cambio de correo por este recorrido obligatorio:

1. El correo de aviso muestra la direccion anterior y la nueva, identifica el documento de inicio de sesion y explica los pasos.
2. Su enlace apunta solamente a `/auth/password-recovery`, sin token ni correo prellenado. La direccion base se obtiene de `FACELIT_ACCESS_URL` como antes; debe configurarse con la URL publica correcta en el entorno de despliegue.
3. El usuario ingresa la nueva direccion y recibe en ella un codigo de seis digitos que vence en cinco minutos.
4. `/api/auth/verify-token` verifica el codigo y registra la verificacion.
5. `/api/auth/reset-password` exige esa verificacion antes de activar el correo y guardar el hash de la nueva contrasena. El acceso posterior usa el numero de documento y la contrasena elegida por el usuario.

Los enlaces directos anteriores de una solicitud `EMAIL_CHANGE` se rechazan en `/api/auth/complete-access`, aunque el token aun sea valido. La bienvenida inicial conserva su mecanismo separado, sin cambiar la generacion de cuentas.

Archivos de esta correccion:

- `AccountAccessService.java`: texto y ruta del aviso, resolucion del correo pendiente, validacion de la solicitud ligada al codigo, activacion final y rechazo de enlaces directos de cambio de correo.
- `PasswordRecoveryServiceImpl.java`: reutiliza la solicitud, envio, verificacion y cambio de contrasena por codigo para el correo pendiente. Invalida los codigos anteriores al solicitar uno nuevo.
- `PasswordRecovery.java`: incorpora la relacion opcional con la solicitud y la fecha de verificacion. Las recuperaciones anteriores conservan esos valores nulos.
- `PasswordRecoveryRepository.java`: comprueba colisiones al generar el codigo, sin cambiar su formato ni los endpoints existentes.
- `VerificationCodeGenerator.java`: conserva el formato de seis digitos y utiliza aleatoriedad criptografica.
- `PasswordRecoveryController.java`: elimina la respuesta alternativa que reenviaba un enlace y devuelve la respuesta normal de codigo enviado.
- `usePasswordRecoveryForm.ts` y `app/auth/password-recovery.tsx`: eliminan la rama que detenía el recorrido del codigo para un envio por enlace. El formulario vuelve a avanzar siempre a la verificacion del codigo.
- `AccountAccessIntegrationTest.java`: adapta los escenarios de cambio de correo al recorrido exigido y agrega pruebas de rechazo por omitir verificacion, enlace directo antiguo, correo incorrecto y reutilizacion del codigo.
- `FaceLit-DB/01_ddl/04_alter/020_email_change_recovery_code.sql` y su entrada en `0000changelog.yaml`: migracion aditiva preparada, no ejecutada. Agrega `access_request_id` y `verified_at` a `security.password_recovery` para impedir que un codigo active otra solicitud o que se omita su verificacion. No elimina ni reescribe datos existentes.

Debe aplicarse la migracion 020 antes de iniciar el backend corregido en un entorno con validacion de esquema. La migracion 019 es su requisito previo si todavia no se ha aplicado.

Validacion de este recorrido: **104 pruebas de backend, 0 fallos, 0 errores y 0 omitidas**, con `BUILD SUCCESS`. Evidencia: `target/email-change-code-tests.log` y los informes en `target/surefire-reports/`. Incluye los tres roles manuales, CSV, verificacion obligatoria, bloqueo del enlace directo antiguo, correo incorrecto, codigo vencido, codigo reutilizado y correccion posterior del destino. Las pruebas usan H2 y correo simulado. TypeScript termino con codigo 0; el script de traducciones mantuvo sus 168 combinaciones correctas. No se aplico la migracion ni se verifico entrega SMTP real.

## Recuperacion del diseno original del correo de bienvenida

La plantilla original seguia disponible en Git dentro de la version anterior de `InitialCredentialEmailListener.java`. Al integrar los reintentos se habia sustituido por el formato generico de notificacion. Se recupera su presentacion para la bienvenida sin revertir el flujo de acceso.

- `InitialCredentialEmailTemplate.java` (nuevo): conserva el encabezado verde, logo con fondo destacado, titulo de credenciales iniciales, tarjetas de nombre/rol/documento/ficha/programa, bloque resaltado y boton. Consulta las asignaciones del mismo usuario al preparar el correo; no agrega columnas ni almacena datos academicos duplicados. Escapa valores y URLs al construir HTML.
- `AccountAccessService.java`: despacha la bienvenida con `EmailService.sendInitialCredentials` y la plantilla recuperada. Los cambios de correo mantienen la recuperacion obligatoria por codigo de seis digitos.
- `EmailServiceImpl.java`: conserva el logo inline y ajusta el texto alternativo para explicar el establecimiento de la contrasena.
- `AccountAccessIntegrationTest.java`: comprueba el envio HTML de bienvenida y conserva las pruebas de reintento y del flujo por codigo.
- `InitialCredentialEmailTemplateTest.java` (nuevo): comprueba datos academicos, ausencia de tarjetas vacias, escape HTML y estructura MIME con logo. Genera `target/welcome-email-preview.html` con datos ficticios y logo incrustado para poder revisar la presentacion.

El bloque de contrasena conserva el aspecto destacado e invita a establecerla mediante el boton. No muestra una contrasena en texto plano, de acuerdo con el requisito anterior. El documento sigue siendo el identificador de inicio de sesion. No hay nuevas migraciones, cambios de permisos, datos historicos ni envios de correo reales.

Validacion: **109 pruebas de backend, 0 fallos, 0 errores y 0 omitidas**, con `BUILD SUCCESS`. Evidencia: `target/welcome-template-tests.log`. La vista previa se genero correctamente en `target/welcome-email-preview.html` con datos ficticios y logo incrustado. Se comprobo el HTML y la estructura MIME; no se realizo una comprobacion visual en Gmail u otro cliente real.

Validacion final de estas tres correcciones:

- Backend completo: **100 pruebas, 0 fallos, 0 errores, 0 omitidas**, `BUILD SUCCESS`. Evidencia: `target/profile-notification-tests.log` y `target/surefire-reports/`.
- Frontend: `tsc --noEmit --pretty false`, codigo de salida 0.
- Traducciones: `node scripts/test-notification-i18n.cjs`, codigo 0; 168 combinaciones de tipos/idiomas, ademas de casos de cambios multiples, valores anteriores/nuevos, responsable, fecha, filtros de secretos, tipo de instructor y compatibilidad con notificaciones antiguas.
- Pruebas nuevas de backend: cinco casos del servicio de avisos y un caso de cambio simultaneo de nombre/tipo de instructor; se ampliaron las pruebas de los tres roles para comprobar el rol del correo y los cambios de nombre en los metadatos.
- `git diff --check` sin errores en los archivos revisados.
