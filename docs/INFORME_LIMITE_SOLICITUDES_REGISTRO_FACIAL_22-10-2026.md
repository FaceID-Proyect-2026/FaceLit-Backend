# Informe de cambios: límite de solicitudes de nuevo registro facial

**Fecha:** 22 de octubre de 2026

**Repositorio:** `FaceLit-Backend`

**Rama al realizar el cambio:** `dev`

**Alcance:** agregar únicamente un tiempo mínimo de siete días entre las solicitudes de un mismo aprendiz al coordinador para autorizar un nuevo registro facial.

## 1. Qué se implementó

Se agregó una validación en el Backend que consulta la última solicitud guardada del aprendiz y comprueba si ya pasaron siete días completos desde su fecha `requested_at`.

- Si todavía no se cumplen los siete días, se rechaza el intento antes de guardar otra solicitud.
- El rechazo no genera una nueva solicitud ni una nueva notificación al coordinador.
- Los intentos bloqueados no modifican la fecha original ni reinician el plazo.
- El límite sigue aplicando aunque la solicitud anterior haya sido aprobada o rechazada, o aunque el aprendiz ya haya registrado otra vez su rostro usando esa autorización.
- El plazo se consulta desde la base de datos, por lo que no depende de la sesión, del navegador ni del dispositivo.
- Se reutiliza el bloqueo por usuario que ya existía para serializar solicitudes simultáneas y evitar duplicados por varios clics.

El punto de inicio es la solicitud aceptada y guardada por el Backend. No se calcula desde la aprobación del coordinador, el nuevo registro del rostro ni la recepción de un correo electrónico.

## 2. Mensaje y respuesta

Cuando el intento ocurre dentro del plazo, el Backend devuelve HTTP **429 — Too Many Requests**, con el mensaje:

> Ya enviaste una solicitud al coordinador. Podrás enviar una nueva el [fecha y hora] (hora de Colombia).

La fecha se presenta con formato `dd/MM/yyyy HH:mm:ss`, usando la zona `America/Bogota`.

No fue necesario modificar el Frontend: el controlador existente devuelve el mensaje en el campo `message`, y la pantalla del aprendiz ya lo muestra cuando falla el envío de una solicitud.

## 3. Archivos modificados y por qué

Las siguientes rutas son relativas a `FaceLit-Backend`.

| Archivo | Estado | Cambio y motivo |
|---|---|---|
| `src/main/java/com/FaceLit/backend/facial/FaceResetService.java` | Modificado | Se agregó la comprobación de siete días antes de crear otra solicitud y el mensaje con su fecha de disponibilidad. Se reutilizan la consulta de la última solicitud y el bloqueo existentes. Se registra la fecha de la solicitud nueva justo antes de guardarla. |
| `src/test/java/com/FaceLit/backend/facial/FaceResetServiceTest.java` | Modificado | Se actualizaron dos expectativas que cambiaron por la nueva regla: un intento reciente se bloquea con 429 y un rechazo ya no permite solicitar inmediatamente. Se conserva la prueba de creación posterior al plazo y las pruebas de aprobación, permisos y notificaciones existentes. |
| `src/test/java/com/FaceLit/backend/facial/FaceResetCooldownTest.java` | Creado | Se agregaron pruebas con un reloj fijo para comprobar límites exactos, estados de la solicitud anterior, mensajes, intentos fallidos y conservación del tiempo original. |
| `src/test/java/com/FaceLit/backend/facial/FaceResetCooldownIntegrationTest.java` | Creado | Se agregaron pruebas de concurrencia y persistencia con filas y bloqueos reales en H2. Permiten verificar que dos envíos simultáneos no creen dos solicitudes y que otra instancia del servicio respete el plazo guardado. |

Además, `docs/INFORME_LIMITE_SOLICITUDES_REGISTRO_FACIAL_22-10-2026.md` es este informe nuevo. Se creó para explicar el cambio sin editar los informes anteriores.

**El único archivo de funcionamiento de la aplicación modificado fue `FaceResetService.java`.** Los otros cambios corresponden a pruebas y a este documento.

### Uso del reloj en el servicio

Se agregó un `Clock` interno. En funcionamiento normal usa la hora del servidor en UTC; el mensaje la convierte a hora de Colombia. Las pruebas pueden proporcionar un reloj fijo para comprobar el instante exacto de los siete días sin depender de esperas reales ni de la hora en que se ejecuten.

## 4. Comportamiento esperado

| Caso | Resultado |
|---|---|
| Primera solicitud, sin antecedentes | Continúa con las validaciones existentes y puede guardarse. |
| Otra solicitud antes de siete días completos | HTTP 429, con fecha y hora de disponibilidad. No se crea otra solicitud. |
| Solicitud anterior rechazada dentro de los siete días | El límite permanece. |
| Solicitud anterior aprobada dentro de los siete días | El límite permanece. |
| Autorización anterior ya utilizada para registrar el rostro | El límite permanece hasta completar los siete días desde la solicitud original. |
| Varios intentos bloqueados | Conservan la fecha original; no reinician el tiempo. |
| Dos envíos simultáneos | El bloqueo existente serializa las operaciones y evita solicitudes duplicadas. |
| Cerrar sesión, recargar o usar otro dispositivo | El Backend consulta el mismo historial guardado y aplica el mismo plazo. |
| Se cumplen exactamente siete días | Termina este límite temporal; se evalúan las demás validaciones existentes. |

**Se conserva la regla anterior de solicitud pendiente o autorización vigente.** Cumplir siete días no anula esas validaciones: si la solicitud sigue pendiente o existe una autorización utilizable, el servicio mantiene su rechazo anterior con HTTP 409. Este ajuste agrega un mínimo de espera; no cambia el proceso de resolución del coordinador.

## 5. Qué no se modificó

- El registro facial y las validaciones del rostro.
- La captura con cámara, el óvalo de guía y la secuencia de retos.
- La aprobación o rechazo del coordinador y el consumo de autorizaciones.
- Los permisos y la configuración de seguridad.
- La recuperación o el restablecimiento de contraseñas.
- El envío existente de notificaciones y sus plantillas.
- El Frontend, la landing y la entrada móvil implementada anteriormente.
- Los informes y documentos anteriores compartidos como referencia.
- Las tablas, columnas y migraciones. Se usa `requested_at`, que ya existía.

En particular, no se tocaron los archivos de la compañera:

- `FaceLit/features/auth/components/FaceGuideOverlay.tsx`
- `FaceLit/features/auth/components/WebCamera.tsx`
- `FaceLit/features/auth/hooks/useFacialRegistration.ts`
- `FaceLit/features/facial/useSessionAttendanceCapture.ts`

Sus hashes SHA-256 coinciden con los valores comprobados anteriormente.

## 6. Pruebas realizadas

**Resultado final: 21 pruebas ejecutadas, sin fallos ni errores.**

| Clase de prueba | Cantidad | Qué comprueba |
|---|---:|---|
| `FaceResetCooldownTest` | 10 | Estados pendiente, rechazado y aprobado; autorización utilizada; bloqueo hasta un nanosegundo antes del plazo; aceptación exactamente al cumplirlo; equivalencia entre zonas horarias; intentos repetidos y fallidos; validación anterior de pendientes; mensaje HTTP para el Frontend. |
| `FaceResetCooldownIntegrationTest` | 2 | Dos solicitudes concurrentes sobre un usuario con bloqueo real en H2 y lectura del plazo persistido desde otra instancia después de aprobar y consumir una autorización. |
| `FaceResetServiceTest` | 9 | Comportamientos existentes de aprobación, rechazo, permisos, autorización, notificaciones y reintentos de entrega, ajustando las expectativas afectadas por el nuevo límite. |

Las pruebas de integración utilizaron H2 y simularon la elegibilidad facial y la asignación académica. Las filas de usuarios, las solicitudes, las transacciones y los bloqueos de concurrencia sí se comprobaron en esa base de prueba. No se usaron datos reales ni se enviaron correos reales.

Evidencia local: `target/face-reset-cooldown-final-tests.log` y los resultados de Surefire en `target/surefire-reports`.

También se comprobó `git diff --check` sobre el código y las pruebas, sin errores.

## 7. Qué conservar al integrar otros cambios

1. La comprobación de siete días en `FaceResetService.create`, antes de guardar una solicitud nueva.
2. La consulta por el usuario autenticado y el bloqueo por usuario existente, dentro de la transacción.
3. El uso de `requested_at` de la última solicitud, sin reiniciarlo en intentos bloqueados, aprobaciones, rechazos o registros posteriores.
4. El mensaje con la fecha y hora de disponibilidad y la respuesta HTTP 429.
5. Las validaciones anteriores de solicitud pendiente, autorización vigente, rostro registrado y ficha activa.
6. Las pruebas de límites temporales, concurrencia y persistencia.

Si un cambio posterior afecta estos puntos, debe coordinarse antes de sobrescribirlos. Los documentos de referencia y los archivos de reconocimiento facial de la compañera deben mantenerse intactos.

## 8. Estado de entrega

Los cambios están locales en el Backend, en la rama `dev` al elaborar este informe. Todavía no se ha hecho commit, push ni despliegue de este ajuste. No se ejecutaron migraciones ni se modificó la base de datos real.

Los archivos de caché de Maven que ya aparecían como modificados o sin seguimiento antes de implementar este límite no forman parte de esta funcionalidad.
