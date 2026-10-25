|# Correcciones y nuevos avisos

Implementación del 16/10/2026. Los correos conservan español; los textos y etiquetas de la app tienen español, inglés, francés y alemán. No requiere una migración nueva: utiliza las tablas de eventos, solicitudes y notificaciones ya existentes.

## Flujos corregidos

- **Ausencia confirmada:** se espera hasta `shutdownTime`, porque antes el aprendiz todavía puede registrar una entrada tardía. Cada minuto se revisan hasta 100 eventos ENTRY ausentes sin excusa que no tengan aviso persistido. Se bloquea y revalida cada evento antes de enviar al aprendiz. Un error queda pendiente para el siguiente ciclo. La consulta incluye ausencias históricas todavía no notificadas; no crea ausencias para sesiones que nunca inicializaron su lista.
- **Salida anticipada:** al abrir la cámara, el instructor elige modo normal o salida anticipada. Una captura en modo anticipado solicita EXIT expresamente. Debe existir una entrada puntual/tardía anterior, estar dentro del horario de sesión y no existir salida previa. El aviso llega al instructor seleccionado como responsable. Una captura repetida en modo normal no se convierte por sí sola en salida.
- **Intentos fallidos y bloqueo:** se confirma la transacción ante `LoginException`, conservando contador y aviso. Otros errores mantienen rollback. El aviso de eliminación bloqueada utiliza una transacción independiente para sobrevivir al rechazo de la eliminación.
- **Excusas:** reciben el aviso el aprendiz y `instructorScheduled`; el mensaje conserva fecha de la sesión original. Repetir una excusa registrada no vuelve a avisar. Retirarla produce `attendance_corrected` con el cambio y el nombre de quien lo hizo.
- **Primer registro facial:** después de que el microservicio confirma el guardado, la app llama `POST /api/facial/registration-confirmation`. Este endpoint no acepta un usuario arbitrario: usa el principal autenticado y verifica el rostro activo. Bloquea la fila del usuario y comprueba confirmaciones anteriores. Los reemplazos se notifican mediante su solicitud de un solo uso. Si la app pierde conexión tras guardar, vuelve a intentar la confirmación al refrescar el estado facial. El fallo de aviso no vuelve a habilitar la captura ni presenta como fallido un embedding ya guardado.

## Nuevos avisos

| Evento | Destinatarios | Canal | Control de repetición |
|---|---|---|---|
| `attendance_corrected` | Aprendiz y responsable seleccionado | App + correo | Guardar exactamente el mismo estado/hora/excusa no genera otro aviso |
| `facial_authorization_expired` | Aprendiz solicitante | App | Un aviso por solicitud, bajo bloqueo y con comprobación persistida; excluye permisos consumidos |
| `password_changed` tras recuperación | Titular de la cuenta | App + correo | Solo después de validar y consumir el código; nunca incluye contraseña ni código |
| `user_role_changed` | Titular de la cuenta | App + correo | No avisa por asignación inicial ni si el rol no cambió |
| `email_delivery_failed` | Coordinador resolutor si sigue teniendo el rol; en su defecto el equipo de coordinación | App | Un aviso por correo fallido y coordinador; solo FAILED con al menos tres intentos, no SENT ni CANCELLED |

El aviso de correo fallido se revisa cada minuto y no genera otro correo. Conserva el estado de entrega original y muestra tipo de aviso, usuario afectado y número de intentos. Cuando no existe un coordinador asignado en el evento, el sistema no inventa un responsable: informa al equipo de coordinación.

El vencimiento facial conserva rostro anterior y decisión histórica. El aprendiz debe solicitar otra autorización; nunca se reactiva la cámara por el simple vencimiento.

## Corrección de asistencia en backend

El nuevo endpoint permite conectar una interfaz de edición de estado/hora sin generar avisos desde el cliente. En esta entrega, el retiro de excusas de la interfaz existente ya genera el aviso automáticamente; no se añadió una pantalla nueva de edición de horas.

`PATCH /api/facial/events/{idFacialEvent}/correction`

```json
{
  "attendanceStatus": "LATE",
  "eventDatetime": "2026-10-16T08:25:00-05:00",
  "excuse": false,
  "reason": "Se verificó la hora de entrada con el instructor responsable"
}
```

- Solo coordinador o instructor vinculado a esa sesión puede corregir; el actor se toma de la autenticación.
- Se bloquea el evento, se exige motivo y se valida el estado contra el horario. No se permiten horas futuras, entrada después del límite ni salida sin entrada anterior.
- Una entrada no puede pasar a ausente si ya tiene salida. Cambiar la hora de entrada no puede contradecir la salida existente. El estado derivado de una salida se mantiene sincronizado cuando se corrige su entrada.
- La notificación conserva estado/hora/excusa anteriores y nuevos, fecha de sesión, aprendiz, ficha, instructor, nombre e identificador del actor, momento y motivo de corrección. El usuario ve nombres; los identificadores quedan para trazabilidad.
- Evento corregido y avisos se guardan en la misma transacción. SMTP se procesa después mediante la bandeja de entrega y los reintentos existentes.

## Puesta en marcha

En `FaceLit-Backend`, reiniciar con la configuración Maven que utiliza el proyecto:

```powershell
mvn clean spring-boot:run
```

Recargar la app para utilizar el selector de captura y las traducciones nuevas. Mantener configurado SMTP para correos reales. Las pruebas de esta entrega simulan correo; no se enviaron mensajes de prueba a usuarios reales.

Los cinco tipos que solo estaban declarados (`attendance_no_exit`, `attendance_wrong_env`, `attendance_session_summary`, `session_cancelled`, `attendance_substitute`) siguen sin un emisor adicional: no formaban parte de esta autorización. La configuración para otro instructor ya tiene su aviso propio, evitando duplicarlo con `attendance_substitute`.

## Evidencia de verificación

- Backend: 71 pruebas aprobadas, cero fallos y cero errores. Incluyen consulta real de ausencias y permisos vencidos en H2, supervivencia del aviso a rollback de eliminación, confirmación del contador/bloqueo de inicio de sesión, destinatarios, repeticiones sin cambios, recuperación de contraseña, corrección de entrada/salida y regresión de CSV, sesiones y recordatorio de salida.
- Frontend: TypeScript sin errores; 168 combinaciones (42 tipos por cuatro idiomas), con comprobaciones del contenido de correcciones, roles y correos fallidos; 13 casos de bloqueo de captura y autorización de un solo uso.
- Correo simulado en las pruebas, incluida la integración CSV. La comprobación no sustituye una prueba con cámara física y entrega SMTP real.
