# Recordatorio de salida: solo correo

El backend revisa cada 15 segundos las sesiones activas cuya hora de salida
está entre ahora y los próximos cinco minutos. La referencia es `exit_time`,
no el tiempo de registro de entrada ni la hora de apagado.

Destinatarios:

- Aprendices con evento ENTRY de esa sesión en estado PUNCTUAL o LATE.
  Se excluyen inasistencias, registros eliminados y entradas con fecha futura.
- El instructor seleccionado en la configuración (`instructorScheduled`).
  En este modelo `instructorInCharge` puede representar a quien abrió la sesión;
  por petición del usuario no se utiliza para este recordatorio.

El correo incluye ficha, nombre del instructor seleccionado, ambiente, fecha
de la sesión y fecha/hora de salida en America/Bogota. El instructor recibe
un texto que le recuerda avisar a sus aprendices. El aprendiz recibe la indicación
de registrar su salida a la hora configurada.

El canal EMAIL usa las tablas notification/email_log para entrega y auditoría,
pero no aparece en la bandeja de la app ni se incluye al marcar todos como leídos.
La programación se deduplica por sesión, horario, contexto y destinatario bajo
bloqueo de la sesión. Durante la ventana se comprueban nuevas llegadas para incluir
aprendices tardíos que aún no habían recibido el aviso.

Antes de enviar o reintentar se valida nuevamente el horario, la sesión activa,
el instructor seleccionado, la ficha, el ambiente y la asistencia. Un aviso
obsoleto se marca CANCELLED y no se envía. Los reintentos de estos correos se
revisan cada 15 segundos, con el máximo existente de tres intentos. El bloqueo
del registro de email evita que dos procesos lo despachen simultáneamente.

La precisión de programación depende del ciclo de 15 segundos y de que el
backend esté ejecutándose; la recepción también depende del servicio SMTP.
Si una sesión se configura dentro de los últimos cinco minutos, se procesa
en el próximo ciclo. Un correo que ya se entregó no puede retirarse tras
un cambio posterior de horario.

## Base de datos

Migración aplicada en PostgreSQL local:
`FaceLit-DB/01_ddl/04_alter/018_email_only_exit_reminders.changelog.yaml`.
Agrega EMAIL al canal permitido, CANCELLED al estado de entrega y convierte
los recordatorios de salida existentes al canal solo correo. Incluye rollback.
Puede ejecutarse por separado del changelog principal, que tenía diferencias
de checksum previas a este cambio.

## Verificación

- ExitReminderServiceTest: seis pruebas de ventana, destinatarios, instructor
  seleccionado frente al creador, duplicados, llegadas posteriores,
  reprogramación y cambios que invalidan el aviso.
- ExitReminderEmailTest: cinco pruebas de envío simulado, cancelación,
  reintento, exclusión de la app y persistencia de la cola de correo.
- ExitReminderRepositoryTest: dos pruebas con JPA y base H2 real en memoria;
  verifica las consultas de horarios y asistentes, incluidas inasistencias,
  entradas futuras, registros eliminados y eventos de solo salida.
- Regresiones: FaceResetServiceTest y FacialAttendanceNotificationTest.

No se enviaron correos reales durante las pruebas. Para la comprobación manual,
reiniciar el backend, configurar una sesión con salida futura, registrar
una entrada puntual, una tardía y una inasistencia. Comprobar que cinco minutos
antes reciben correo los dos asistentes y el instructor seleccionado, y que
ninguno de estos avisos aparece en la app.
