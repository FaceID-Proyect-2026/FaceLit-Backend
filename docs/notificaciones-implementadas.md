# Notificaciones Implementadas Actualmente

Todas las notificaciones viven en el monolito, en `notification.notification`.
Cuando el canal es `APP_EMAIL`, tambien se crea el registro en
`notification.email_log` y el correo se dispara de forma asincrona despues del
commit.

## Reglas Generales

- La campana consulta `GET /api/notifications`.
- Cada usuario ve solo las notificaciones asociadas a su `id_user_app`.
- Solo los tipos del catalogo marcados como `APP_EMAIL` generan correo.
- El correo no incluye contrasenas, tokens ni datos biometricos; solo describe el evento.
- Si el envio de correo falla, se registra en `email_log` y la notificacion sigue visible en la app.
- Las notificaciones que dependen del micro llegan al monolito por API; el micro no guarda notificaciones.

## Ya Conectadas En El Monolito

| Tipo | Destinatario | Canal | Evento que la genera |
|---|---|---|---|
| `csv_upload_done` | Coordinador | App | Termina `POST /api/academic/csv/upload`. |
| `csv_inconsistency` | Coordinador | App + correo | Una fila CSV queda bloqueada por inconsistencia. |
| `csv_ref_error` | Coordinador | App | Una fila CSV referencia programa/ficha/instructor inexistente. |
| `csv_transfer_applied` | Coordinador | App | El coordinador acepta un traslado pendiente de CSV. |
| `csv_transfer_rejected` | Coordinador | App | El coordinador cancela un traslado pendiente de CSV. |
| `learner_transferred` | Coordinador e instructores de la ficha anterior/destino | App | Traslado manual de aprendiz entre fichas del mismo programa. |
| `apprentice_transfer_applied` | Aprendiz trasladado | App | Su ficha fue cambiada por coordinacion. |
| `academic_delete_blocked` | Coordinador | App | Se intenta eliminar programa/ficha/instructor con relaciones activas. |
| `instructor_profile_updated` | Instructor | App | Coordinacion cambia datos academicos del instructor. |
| `instructor_assignment_updated` | Instructor | App | Coordinacion cambia programas/asignacion academica del instructor. |
| `security_multiple_failures` | Coordinador | App + correo | Una cuenta acumula varios intentos fallidos. |
| `security_account_locked` | Coordinador | App + correo | Una cuenta queda bloqueada por superar el limite de intentos. |

## Preparadas Para El Microservicio

Estas existen en el catalogo y se pueden recibir por API, pero el evento real lo
debe producir el micro de reconocimiento/asistencia cuando este listo.

| Tipo | Destinatario esperado | Canal |
|---|---|---|
| `attendance_absent` | Instructor/aprendiz segun caso | App |
| `attendance_late` | Instructor/aprendiz segun caso | App |
| `attendance_early_exit` | Instructor | App |
| `attendance_no_exit` | Instructor/aprendiz segun caso | App |
| `attendance_wrong_env` | Coordinador/instructor segun caso | App + correo |
| `attendance_substitute` | Coordinador | App |
| `facial_reregister_request` | Coordinador | App |

## RF-8.4 Reconocimiento Facial

El monolito ya tiene el tipo `facial_reregister_request` y la accion de la UI
para aceptar/rechazar desde la notificacion. Cuando exista el micro:

1. El micro crea `face_reset_request`.
2. El micro avisa al monolito para crear `facial_reregister_request`.
3. El coordinador acepta o rechaza en la campana.
4. El monolito llama al micro para resolver la solicitud.

Mientras el micro no exista, esa notificacion queda como contrato preparado.
