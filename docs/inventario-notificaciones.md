# Inventario de notificaciones de FaceLit

Revisión del código: 16/10/2026. Actualizado con los cinco avisos propuestos ya implementados, sus destinatarios, canales, datos y acciones recomendadas. Incluye los ajustes previos de nombres y sesiones. La existencia de un emisor no demuestra la entrega real de correos en producción; las pruebas de envío utilizan correo simulado.

El catálogo contiene 42 tipos: 37 tienen código emisor y 5 solo están declarados. La confirmación de contraseña recuperada reutiliza `password_changed`. App + correo significa que se crea notificación de app y registro de entrega de correo; requiere SMTP operativo y dirección válida.

## Tipos con código emisor

### Cinco avisos propuestos: implementados

Los cinco avisos siguientes ya tienen emisor en backend. No son ejemplos de una funcionalidad pendiente. Las etiquetas, instrucciones y fechas de la app se traducen a español, inglés, francés y alemán; el correo conserva español. Los identificadores técnicos se conservan como trazabilidad, no como nombres visibles.

| Notificación implementada | Destinatario y motivo de recepción | Información del aviso y acción recomendada | Canal |
|---|---|---|---|
| **Asistencia corregida** (`attendance_corrected`) | Aprendiz cuya asistencia cambió e instructor seleccionado para esa sesión, cada uno identificado como destinatario | Nombre del aprendiz, ficha, fecha original de sesión, responsable, entrada/salida afectada, estado/hora/excusa anteriores y nuevos, nombre de quien modificó y motivo. Revisar el registro y contactar a coordinación si hay desacuerdo. | App + correo |
| **Permiso facial vencido sin utilizar** (`facial_authorization_expired`) | Aprendiz titular de la autorización, porque terminó el plazo sin completar el reemplazo | Nombre del aprendiz, ficha, programa y fecha de vencimiento. El rostro anterior se conserva; el permiso vencido no permite abrir la cámara. Solicitar otra autorización al coordinador. | Solo app |
| **Contraseña restablecida mediante recuperación** (`password_changed`, acción `password_recovered`) | Titular de la cuenta, para confirmar un cambio de seguridad completado | Nombre del titular, fecha del cambio y confirmación específica de recuperación. Puede ingresar con su nueva contraseña; debe contactar al coordinador si desconoce el cambio. Nunca incluye contraseña ni código. | App + correo |
| **Rol de usuario modificado** (`user_role_changed`) | Usuario cuyo rol existente cambió, porque cambió su acceso | Nombre del titular, rol anterior y nuevo con nombres legibles, nombre de quien cambió el rol y fecha. Cerrar sesión y volver a ingresar para cargar el nuevo rol; contactar a coordinación si el cambio es inesperado. | App + correo |
| **Correo definitivamente no entregado** (`email_delivery_failed`) | Coordinador resolutor cuando está identificado y conserva el rol; en otros eventos, equipo de coordinación para seguimiento | Nombre del usuario afectado, título/tipo del correo que falló y tres intentos agotados. El aviso en la app se conserva. Revisar dirección y contactar al destinatario; no se inventa una causa técnica no registrada. | Solo app |

**Alcance de la corrección:** retirar una excusa desde la interfaz actual ya emite el aviso. La corrección de estados y horas está disponible mediante `PATCH /api/facial/events/{id}/correction`; esta entrega no incorpora una pantalla nueva para editar horas. Guardar los mismos valores no repite el aviso.

### Ejemplos de lectura para el usuario

Datos ficticios; los valores reales se toman del evento, la sesión, la cuenta y el usuario autenticado.

- **Al aprendiz y al instructor:** “Se corrigió la entrada de Carlos Ramírez en la sesión del 16/10/2026, ficha 3145555, responsable Ana Gómez. Antes: inasistencia, sin excusa. Ahora: retraso, entrada 08:25. Modificado por Ana Gómez. Motivo: entrada verificada. Revisa el registro actualizado.” Cada destinatario recibe además la razón por la que le corresponde el aviso.
- **Al aprendiz con permiso vencido:** “Carlos Ramírez, tu autorización para reemplazar el rostro venció sin utilizarse. Ficha 3145555, programa Análisis y Desarrollo de Software. Permiso vencido el 17/10/2026 a las 09:00, hora de Colombia. Tu rostro anterior se conserva. Solicita otra autorización al coordinador.”
- **Al titular que recuperó su cuenta:** “Carlos Ramírez, tu contraseña fue restablecida mediante recuperación. Recibes este aviso para confirmar un cambio de seguridad en tu cuenta. Puedes ingresar con la nueva contraseña. Si no realizaste el cambio, contacta al coordinador.” El aviso muestra la fecha del cambio.
- **Al usuario cuyo rol cambió:** “Carlos Ramírez, tu rol cambió de Aprendiz a Instructor. Modificado por María Rodríguez. Cierra sesión y vuelve a ingresar para cargar tu nuevo rol. Si no esperabas este cambio, contacta al coordinador.” El aviso muestra la fecha y la cuenta destinataria.
- **Al coordinador de seguimiento:** “No se pudo entregar el correo «Registro facial autorizado» a Carlos Ramírez después de tres intentos. Lo que falló fue la entrega por correo; el aviso de la app se conserva. Revisa la dirección de la cuenta y contacta al destinatario.”

### Catálogo completo

| Tipo | Evento | Destinatario real | Canal |
|---|---|---|---|
| `csv_upload_done` | Finaliza carga CSV | Todos los coordinadores | App |
| `csv_inconsistency` | Conflicto CSV pendiente de revisión | Todos los coordinadores | App + correo |
| `csv_transfer_applied` | Se acepta traslado propuesto por CSV | Todos los coordinadores | App |
| `csv_transfer_rejected` | Se rechaza traslado propuesto por CSV | Todos los coordinadores | App |
| `csv_ref_error` | Error de referencias en filas CSV | Todos los coordinadores | App |
| `learner_transferred` | Cambio de ficha del aprendiz | Coordinadores e instructores vinculados a fichas/programas de origen y destino | App + correo |
| `apprentice_transfer_applied` | Cambio de ficha confirmado | Aprendiz trasladado | App + correo |
| `attendance_absent` | La sesión terminó y el evento ENTRY sigue ausente sin excusa; revisa pendientes cada minuto y evita repetir el aviso | Aprendiz afectado | App + correo |
| `attendance_late` | Entrada registrada con retraso | Aprendiz afectado | App + correo |
| `attendance_early_exit` | Se registra EXIT explícito antes de la hora prevista, con entrada real anterior | Instructor responsable seleccionado (`instructorScheduled`) | App + correo |
| `attendance_excuse_registered` | Excusa pasa de no registrada a registrada | Aprendiz e instructor responsable seleccionado | App + correo |
| `attendance_corrected` | Corrección autorizada de estado/hora de entrada o salida, o retiro de una excusa existente | Aprendiz e instructor responsable seleccionado | App + correo |
| `attendance_exit_reminder` | Faltan hasta cinco minutos para salida configurada | Aprendices con entrada puntual o tardía y el instructor seleccionado en la sesión (`instructorScheduled`) | Solo correo |
| `session_assigned` | Instructor configura sesión para sí mismo | Instructor autenticado, que también es el responsable seleccionado | App + correo |
| `facial_session_substitution` | Instructor configura sesión para otro instructor | Instructor autenticado que configura e instructor responsable seleccionado; una notificación para cada uno | App + correo |
| `session_updated` | Cambia ambiente, ficha, responsable u horarios | Instructor que modifica, responsable seleccionado actual, responsable seleccionado anterior y aprendices activos de la ficha actual, sin duplicados | App + correo |
| `academic_delete_blocked` | Se intenta eliminar registro con dependencias; aviso guardado en transacción independiente | Todos los coordinadores | App |
| `academic_entity_created` | Se crea usuario, aprendiz o instructor mediante servicios que emiten el aviso | Todos los coordinadores | App |
| `academic_entity_updated` | Se modifican datos de usuario, aprendiz o instructor mediante esos servicios | Todos los coordinadores | App |
| `academic_entity_activated` | Se reactiva usuario, aprendiz, instructor, ficha o programa mediante los flujos conectados | Todos los coordinadores | App |
| `academic_entity_deactivated` | Se desactiva usuario, instructor, ficha o programa mediante los flujos conectados | Todos los coordinadores | App |
| `academic_entity_deleted` | Eliminación definitiva de ficha o programa | Todos los coordinadores | App |
| `user_account_created` | Cuenta creada | Titular de la nueva cuenta: aprendiz, instructor o coordinador según la cuenta | App |
| `user_profile_updated` | Datos personales modificados desde gestión general de usuarios | Titular de la cuenta afectada | App |
| `instructor_profile_updated` | Datos del instructor modificados manualmente o por CSV | Instructor afectado | App |
| `instructor_assignment_updated` | Cambia asignación académica del instructor | Instructor afectado | App |
| `apprentice_profile_updated` | Datos del aprendiz modificados manualmente o por CSV | Aprendiz afectado | App |
| `facial_reregister_request` | Aprendiz solicita permiso para reemplazar rostro | Todos los coordinadores; solo coordinador puede resolver | App + correo |
| `facial_request_resolved` | Coordinador acepta o rechaza solicitud facial | Aprendiz solicitante | App + correo |
| `facial_registration_completed` | Confirmación contra rostro persistido tras registro del microservicio, registro Java o reemplazo autorizado completado | Usuario que registró su rostro | App |
| `facial_authorization_expired` | Permiso aceptado vence sin consumirse; se conserva el rostro anterior | Aprendiz solicitante | App |
| `password_changed` | Cambio de contraseña o recuperación completada correctamente | Titular de la cuenta | App + correo |
| `user_role_changed` | Cambia un rol existente; conserva rol anterior, nuevo y actor. No avisa por asignación inicial ni por guardar el mismo rol | Usuario afectado | App + correo |
| `email_delivery_failed` | Correo con estado FAILED tras agotar tres intentos | Coordinador que resolvió la solicitud, si sigue siendo coordinador; en otros casos, todos los coordinadores | Solo app |
| `account_status_changed` | Desactivación de cuenta en los flujos conectados | Titular de la cuenta afectada | App + correo |
| `security_multiple_failures` | Intentos de inicio fallidos desde el segundo y antes del máximo; se conservan aunque falle la autenticación | Todos los coordinadores | App + correo |
| `security_account_locked` | Se alcanza máximo de intentos fallidos | Todos los coordinadores | App + correo |

## Tipos declarados sin emisor automático en backend

Estos canales son los del catálogo; actualmente no hay un destinatario efectivo porque no hay envío conectado. Los ejemplos o simulaciones del frontend no equivalen a una entrega del backend.

| Tipo | Finalidad declarada | Canal declarado |
|---|---|---|
| `attendance_no_exit` | Salida no registrada | App |
| `attendance_wrong_env` | Registro en sesión/ambiente incorrecto | App + correo |
| `attendance_session_summary` | Resumen de asistencia de la sesión | App + correo |
| `session_cancelled` | Cancelación de sesión | App + correo |
| `attendance_substitute` | Sesión abierta por suplencia | App |

## Correcciones implementadas después de la inspección

1. **Inasistencia:** `ConfirmedAbsenceDelivery` consulta eventos ausentes al llegar la hora de apagado. Revalida bajo bloqueo, excluye excusas y entradas reales, y consulta el aviso persistido para evitar duplicados. También recupera eventos históricos pendientes de aviso. La inicialización sigue creando ausencias provisionales al abrir la cámara; no se notifica ausencia antes del cierre.
2. **Salida anticipada:** la cámara permite elegir registro normal o salida anticipada. El modo anticipado envía `eventType=EXIT`. El servidor exige entrada real anterior, hora dentro de sesión y un solo evento de salida; el modo normal mantiene sus ventanas habituales.
3. **Intentos fallidos y bloqueo:** la excepción esperada de autenticación ya no revierte el contador ni los avisos. Se mantiene rollback para fallos inesperados.
4. **Eliminación bloqueada:** la notificación se guarda con `REQUIRES_NEW`, independiente de la operación académica rechazada.
5. **Primer registro facial:** tras guardar el embedding, la app invoca confirmación autenticada. El servidor verifica el rostro activo en base de datos, serializa por usuario y evita repetir la confirmación. Si falla el aviso, se reintenta al consultar nuevamente el estado facial. El reemplazo autorizado conserva su entrega persistente con el contexto de la solicitud.
6. **Instructor responsable:** excusas y salidas anticipadas usan siempre `instructorScheduled`, el seleccionado para esa sesión.

Los detalles de operación y del endpoint de corrección están en [notificaciones: correcciones y nuevos avisos](notificaciones-correcciones-y-nuevos-avisos.md).

## Correos transaccionales fuera del centro de notificaciones

| Correo | Destinatario | App |
|---|---|---|
| Código de recuperación/verificación de contraseña | Titular que solicita recuperación | No genera por sí solo un aviso del catálogo |
| Credenciales iniciales de acceso | Titular de cuenta creada mediante el flujo que publica credenciales | Correo independiente del aviso `user_account_created` |

## Cambios de esta entrega

- Decisión facial: se guarda `decidedByName` y se conserva UUID de auditoría en `decidedBy`. La app muestra el nombre. Para avisos antiguos sin nombre, el backend busca el usuario; si ya no está disponible, la app muestra “Coordinador”. Los correos ya enviados no se modifican.
- Configuración de sesión: nombre del instructor autenticado que configuró, nombre del responsable seleccionado, ficha, ambiente, inicio, salida y apagado en hora de Colombia. App traduce las etiquetas en español, inglés, francés y alemán; los correos conservan español.
- Cuando son dos instructores distintos se informa a ambos por app y correo sin enviar además un aviso de asignación duplicado. Si son la misma persona se envía un solo aviso.
- Una actualización identifica el responsable seleccionado anterior y el nuevo, incluso cuando quien modifica sigue siendo el mismo instructor.

## Referencias de implementación

Rutas relativas a `src/main/java/com/FaceLit/backend/`:

- Catálogo: `notification/service/NotificationCatalog.java`.
- Persistencia, destinatarios por rol y canales: `notification/service/serviceImpl/NotificationServiceImpl.java`.
- Envíos/reintentos: `notification/service/serviceImpl/EmailDispatchServiceImpl.java`.
- CSV: `academic/service/serviceImpl/academic/CsvAcademicServiceImpl.java`.
- Traslados y cambios de aprendices: `academic/service/serviceImpl/academic/UserChipServiceImpl.java`.
- Cambios académicos: servicios `ProgramServiceImpl`, `ChipServiceImpl`, `InstructorServiceImpl` y gestión de usuarios.
- Asistencia y excusas: `facial/service/serviceImpl/FacialEventServiceImpl.java`.
- Sesiones: `environment/service/serviceImpl/EnvironmentServiceImpl.java`.
- Recordatorio de salida: `environment/service/ExitReminderService.java`.
- Solicitud/decisión/reemplazo facial: `facial/FaceResetService.java`, `facial/FaceResetNotificationDelivery.java`.
- Seguridad de inicio: `auth/service/serviceImpl/roleandpermission/LoginServiceImpl.java`.

La traducción y presentación se encuentran en frontend, `features/notifications/`, `app/notifications/index.tsx` y `shared/i18n/`.

Verificación final: 71 pruebas del backend aprobadas, incluidas transacciones reales en H2, consultas JPA de pendientes y regresión de CSV/sesiones/recordatorio de salida; TypeScript sin errores; 168 combinaciones de tipo/idioma y 13 casos de acceso a captura aprobados. Los casos de envío utilizan correo simulado, incluida la integración CSV. No se verificó captura física con cámara ni entrega SMTP real a usuarios.

Revisión posterior de claridad: 25 pruebas específicas de los cinco avisos aprobadas nuevamente, con nombres de destinatarios, fechas, motivo de recepción e instrucciones. El aviso de recuperación tiene texto propio en la app; el permiso vencido usa la etiqueta “Permiso vencido el”, para no confundirse con una autorización vigente.
