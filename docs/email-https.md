# Correo HTTPS en Render

Los tres flujos usan `EmailService` y un transporte compartido a
`https://api.resend.com/emails`. No se necesita SDK ni SMTP. Se conservan las
plantillas, texto alternativo y logo PNG base64 con `content_id=facelitLogo`.
Conexion: 5 segundos; respuesta: 15 segundos. Sin redirecciones ni reintentos
en el transporte. Cualquier estado fuera de 2xx produce un error sanitizado.
La API key, cuerpos y respuestas del proveedor no se registran en logs.

## Requisitos externos

Segun la documentacion oficial consultada el 16 de octubre de 2026, Resend Free
permite 3.000 correos al mes, hasta 100 al dia, sin tarjeta. No es ilimitado.
El alta normal no requiere esperar una aprobacion manual, aunque hay controles
antiabuso y una cuenta restringida debe resolverse con el proveedor.

Para destinatarios reales se necesita un dominio propio o subdominio bajo tu
control, verificado por DNS. No puedes verificar gmail.com ni utilizar una
direccion Gmail como remitente autorizado. `onboarding@resend.dev` solo sirve
para pruebas al correo del propietario; no habilita el envio a todos los usuarios.
Si no tienes acceso a un dominio, ese requisito queda pendiente: no se ha creado
ninguna cuenta, comprado dominios ni contratado planes.

Fuentes oficiales:
- [Precios](https://resend.com/pricing)
- [Sin tarjeta, correo transaccional](https://resend.com/products/transactional-emails)
- [Alta y controles antiabuso](https://resend.com/handbook/success/what-is-our-philosophy-for-anti-abuse)
- [Dominios y DNS](https://resend.com/docs/dashboard/domains/introduction)
- [Restriccion del dominio de pruebas](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain)
- [API keys](https://resend.com/docs/dashboard/api-keys/introduction)
- [API de envio](https://resend.com/docs/api-reference/emails/send-email)
- [Logo inline CID](https://resend.com/changelog/embed-images-using-cid)

## Configurar y redesplegar

1. En tu cuenta Resend, abre Domains y agrega un dominio/subdominio de envio.
2. En su proveedor DNS publica exactamente los registros SPF y DKIM indicados
   por Resend. Configura DMARC segun sus recomendaciones. Espera estado Verified.
3. En API Keys crea una clave con permiso Sending access limitada al dominio.
   Copiala una sola vez a Render; no la pegues en commits, chats ni logs.
4. En el servicio **Java** de Render, Environment, configura:

   ```dotenv
   RESEND_API_KEY=re_clave_privada_real
   FACELIT_EMAIL_FROM=notificaciones@correo.tu-dominio.example
   FACELIT_EMAIL_FROM_NAME=FaceLit
   FACELIT_ACCESS_URL=https://tu-aplicativo-publicado.example
   ```

   El remitente debe pertenecer al dominio verificado. La URL de la API es fija;
   no hay variable de endpoint. `.env.example` contiene solamente las variables
   de correo y enlace, no sustituye las demas variables existentes del backend.
   Retira MAIL_USERNAME y MAIL_PASSWORD si estaban destinados solamente a SMTP.
   Conserva DB_*, JWT_SECRET y la configuracion facial existente.
5. Publica el commit revisado en la rama que despliega el backend. En Render,
   Manual Deploy > Deploy latest commit, o guarda variables y redespliega.
   Usa el Dockerfile Java existente. Confirma el arranque y ausencia de errores
   de configuracion; no se necesita ningun bean JavaMailSender.

La capa de correo puede arrancar sin claves para permitir pruebas locales;
intentar enviar sin ellas falla explicitamente. El arranque completo conserva
sus requisitos existentes de PostgreSQL y demas configuracion.

## Prueba manual autorizada despues del despliegue

Las pruebas automatizadas usan HTTP local y nunca envian correo real.
Para comprobar la integracion desplegada utiliza una cuenta QA y buzon autorizado:

1. Desde el flujo de alta existente crea una unica cuenta QA, si necesitas validar
   bienvenida. Confirma asunto, datos academicos, credenciales, logo y enlace
   configurado en FACELIT_ACCESS_URL. La bienvenida se dispara con
   InitialCredentialsEmailRequested, de forma asincrona AFTER_COMMIT; sus intentos
   no se registran en notification.email_log. No repitas CSV ni altas masivas.
2. Para una cuenta existente activa usa "olvide mi contrasena". El flujo ya usa
   POST /api/auth/request-recovery con `email`; luego /verify-token con `token`
   y /reset-password con `token`, `newPassword`, `confirmPassword`. Usa el codigo
   recibido antes de cinco minutos y respeta la politica actual de contrasenas.
   No recuperes hashes ni regeneres credenciales iniciales.
3. Genera una notificacion de prueba mediante un flujo existente que corresponda
   al usuario QA. Comprueba asunto y contenido. No alteres los canales del catalogo.
4. Contrasta la aceptacion en Resend con su estado de entrega y el buzon/spam.
   `SENT` significa que la API acepto la solicitud, no entrega final a bandeja.

El despachador conserva sus reintentos y limite preexistentes; no se agregaron
reintentos nuevos. Un timeout puede ser ambiguo si el proveedor ya acepto el
mensaje: el mecanismo preexistente podria repetir una notificacion. No se cambia
ese contrato ni se garantiza entrega exactamente una vez.
No se encontro ruta de reenvio de bienvenida: queda pendiente fuera del alcance.
Los usuarios ya creados deben utilizar recuperacion; no hay reenvio masivo automatico.

## Verificacion automatizada

`mvnw.cmd -Dtest=EmailHttpsTest,ExitReminderEmailTest test`

EmailHttpsTest usa un servidor HTTP local: bienvenida real del listener,
recuperacion, notificacion, logo, configuracion Spring, errores 3xx/4xx/5xx y timeout.
ExitReminderEmailTest comprueba FAILED ante fallo y el comportamiento del despachador.
El contexto aislado valida que la capa de correo arranca sin SMTP; no sustituye
una prueba del arranque completo con PostgreSQL.

En este entorno `mvnw.cmd` fallo inicialmente al indexar `Target[0]` nulo en
PowerShell. Para ejecutar el Wrapper se utilizo una copia ignorada en
`target/qa-wrapper`, con esa comprobacion reemplazada por una comprobacion de
Target no vacio, las mismas propiedades y Maven 3.9.14. No se modifico el Wrapper
versionado. Se indico `-Dmaven.repo.local=<backend>/.m2/repository` y se ejecuto
fuera del aislamiento tras errores AccessDeniedException de Java al leer JAR.

Resultado final: **19 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**.
Incluye rechazo HTTP 403 real del servidor simulado hasta el despachador, que
guarda FAILED. `git diff --check` paso. No se enviaron correos reales ni se
ejecuto el contexto completo contra PostgreSQL. La entrega real y el arranque
completo deben verificarse en el despliegue configurado.
