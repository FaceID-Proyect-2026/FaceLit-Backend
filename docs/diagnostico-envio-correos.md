# Diagnóstico real del envío de correos

Revisión local del 16/10/2026. Lecturas de la base PostgreSQL configurada en el backend, comprobación del proceso que escucha en 8080 y prueba de conexión TLS/autenticación SMTP. No se modificaron destinatarios ni canales y no se enviaron correos de prueba.

## Resultados

- Backend local en ejecución en el puerto 8080. La app tiene configurada la API local en ese puerto; los dispositivos pueden adaptar el host según Expo.
- Conexión SMTP Gmail, TLS y autenticación aceptadas; comando NOOP respondió 250. Esta prueba no envía mensajes.
- 62 notificaciones persistidas: 54 de canal APP y 8 de canal APP_EMAIL.
- Las 8 de APP_EMAIL tienen registro de correo con estado SENT. No hay registros PENDING, FAILED ni CANCELLED, ni notificaciones de canal de correo sin registro de entrega.
- No se detectaron notificaciones de canal de correo cuyo destinatario carezca de credencial/correo en la base revisada.

SENT significa que el proceso de envío al servidor SMTP terminó sin excepción. No demuestra que el proveedor destinatario haya colocado el mensaje en Recibidos: puede estar en spam o verse afectado por reglas del buzón. Las pruebas simuladas anteriores no eran evidencia de entrega real; este informe sí consulta los registros locales reales, con esa limitación.

## Correos de notificaciones registrados

| Tipo | Cantidad aceptada por SMTP |
|---|---:|
| `apprentice_transfer_applied` | 2 |
| `learner_transferred` | 4 |
| `facial_reregister_request` | 1 |
| `facial_request_resolved` | 1 |

## Avisos que no generaron correo por su canal

| Tipo | Cantidad | Canal |
|---|---:|---|
| `academic_entity_created` | 26 | Solo app |
| `user_account_created` | 26 | Solo app |
| `csv_upload_done` | 1 | Solo app |
| `facial_registration_completed` | 1 | Solo app |

Los correos de credenciales iniciales y códigos de recuperación siguen un servicio independiente y no están contabilizados en `notification.email_log`. No se debe interpretar el estado de `user_account_created` como evidencia de entrega o fallo del correo de bienvenida.

## Condiciones de los cinco avisos nuevos

- **Asistencia corregida:** canal app + correo. La base revisada no contiene sesiones ni eventos de asistencia; tampoco hay avisos de este tipo persistidos.
- **Permiso facial vencido sin usar:** solo app, como fue solicitado. Hay una solicitud aceptada, pero cero permisos vencidos sin utilizar. No corresponde generar ese aviso en el estado revisado.
- **Contraseña recuperada:** app + correo al completar el restablecimiento. En los registros consultados no existe un evento `password_changed`; solicitar el código por sí solo no genera la confirmación de contraseña cambiada.
- **Rol modificado:** app + correo cuando cambia un rol existente. No hay eventos `user_role_changed` persistidos. La asignación inicial al crear una cuenta no cuenta como cambio de rol.
- **Correo definitivamente no entregado:** solo app. No corresponde emitirlo en el estado revisado, porque no hay correos FAILED que hayan agotado sus tres intentos.

## Lo que falta para investigar un mensaje no recibido

Identificar título/tipo y hora aproximada del aviso que apareció en la app. Con ello se puede comprobar su canal, destinatario, registro de entrega y momento de envío sin reenviar mensajes masivamente ni cambiar a correo todos los avisos que fueron definidos como solo app.
