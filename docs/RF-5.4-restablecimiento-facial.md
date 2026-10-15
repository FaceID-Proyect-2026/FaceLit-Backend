# RF-5.4: solicitud y autorización de re-registro facial

El aprendiz autenticado solicita el restablecimiento desde Mi reconocimiento facial.
El backend toma su identidad de la sesión y su nombre, ficha y programa de la
asignación académica activa. La solicitud persistida es la fuente de verdad.

Los coordinadores reciben un aviso en la app y un correo con enlace a
`/notifications`. Aceptar o rechazar se realiza únicamente dentro de la app,
con sesión de coordinador y una notificación dirigida a ese destinatario.
La decisión guarda actor y fecha. Las copias de otros coordinadores se actualizan.

La aprobación genera una autorización de un uso válida durante 24 horas
(`facial.reset.authorization-hours`). El token solo se entrega al aprendiz
autenticado mediante `/api/facial/reset-requests/me`; nunca aparece en las
notificaciones ni en el correo. Se mantienen los tres retos de captura en vivo.

El registro anterior se conserva hasta finalizar la nueva captura. El microservicio
valida la autorización, reemplaza el embedding de la misma fila y consume el permiso
en una sola transacción. Un fallo de escritura revierte ambos cambios. No queda
un segundo rostro activo. Rechazar conserva el registro y permite solicitar de nuevo
sin un periodo de espera. Una autorización consumida o vencida no permite otro reemplazo.

La entrega de solicitud, decisión y confirmación de reemplazo se reintenta mediante
campos persistidos en la solicitud. La decisión no depende de que se pueda enviar
un correo; los correos usan los estados y reintentos de `email_log` existentes.

## Preparación

Se añadió la migración `FaceLit-DB/01_ddl/04_alter/017_face_reset_authorization.changelog.yaml`.
Fue aplicada en PostgreSQL local durante esta implementación. El historial principal
presentaba diferencias previas de checksum en cinco cambios de tablas; se ejecutó
solo el changelog nuevo, sin borrar ni recalcular esos checksums.

Para otros entornos, aplicar esta migración antes de iniciar backend y microservicio.
Configurar `FACELIT_ACCESS_URL` con la URL de la app accesible desde el dispositivo
que abre el correo. `localhost` es únicamente la dirección de desarrollo del mismo equipo.
Reiniciar backend y microservicio; recargar la app.

## Evidencia automatizada

La app bloquea nuevamente Iniciar captura en cuanto la API confirma el guardado,
antes de cerrar el mensaje de éxito. Una respuesta antigua no puede reactivar la
solicitud consumida. Las 24 horas son el plazo para usar el permiso una vez, no
un periodo de registros ilimitados. `FaceLit/scripts/test-facial-registration-access.cjs`
verifica este bloqueo, respuestas antiguas, vencimiento y una nueva aprobación.

- `FaceResetServiceTest`: aprobación, actor/fecha, vencimiento, segunda resolución,
  destinatario fuera de alcance, duplicación, nueva solicitud tras rechazo,
  nombre/ficha/programa/enlace y reintento de entrega después de guardar la decisión.
- Regresiones: `FacialEnrollmentControllerTest` y `FacialAttendanceNotificationTest`.
- `micro-facial/tests/test_face_reset_authorization.py`: autorización obligatoria,
  token ligado al propietario, consumo, rollback y captura en vivo obligatoria.
- `micro-facial/tests/test_face_reset_postgres.py`: ejecuta el repositorio real contra
  PostgreSQL local, en un esquema aislado dentro de una transacción que se revierte.
  Verifica reemplazo sobre la misma fila, un solo rostro, consumo, vencimiento,
  token incorrecto y rollback tras un vector inválido.
- `FaceLit/scripts/test-notification-i18n.cjs`: catálogo en es/en/fr/de y mensajes
  de solicitud/aceptación/rechazo con datos académicos. TypeScript sin errores.

Las pruebas de Python se ejecutaron con dependencias en una carpeta temporal,
sin modificar el entorno ni las dependencias de ejecución del microservicio.

## Caso de aceptación manual pendiente

1. Usar un aprendiz con rostro previo y ficha activa. Solicitar restablecimiento.
   Recargar: la solicitud permanece pendiente y no permite duplicarla.
2. Como coordinador, comprobar correo y aviso con nombre, ficha y programa.
   Abrir el enlace e iniciar sesión si corresponde; aceptar desde la notificación.
3. Como aprendiz, comprobar el aviso de aprobación. Abrir Mi reconocimiento facial,
   completar los tres retos con el rostro correcto y verificar la actualización.
4. Intentar reutilizar la autorización: el servidor la rechaza.
5. En otra solicitud, rechazar y comprobar que se permite solicitar nuevamente.
6. Intentar resolver desde instructor/aprendiz u otro destinatario: se deniega.

No se ejecutó una captura física ni se enviaron correos reales como parte de las
pruebas. La conformidad completa del flujo de cámara y SMTP queda pendiente de
ejecutar este caso en las cuentas de prueba.
