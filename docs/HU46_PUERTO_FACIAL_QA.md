# FaceLit — Nota para QA: puerto local del servicio facial

Fecha: 31 de octubre de 2026. Equipo 4. Ambiente: desarrollo local en Windows con microservicio en Docker.

## Qué ocurrió y por qué se cambió

Después de habilitar el ingreso al sistema, la pantalla facial mostró «No fue posible conectar con el servicio facial». El backend estaba activo en 8080, pero la dirección configurada para el microservicio era `http://127.0.0.1:8090`.

La inspección de Windows encontró un proceso `WsToastNotification.exe` de Wondershare escuchando en 8090, además de los procesos de Docker/WSL asociados a ese puerto. Una petición a `http://127.0.0.1:8090/health` devolvió HTTP 501; otra por localhost agotó el tiempo de espera. En cambio, la comprobación de salud ejecutada dentro del contenedor respondió correctamente. Las claves de servicio de los archivos locales coincidían; sus valores no se imprimieron ni se modificaron.

La evidencia señala un conflicto en el acceso al puerto publicado desde Windows. Se eligió 8091, comprobado libre antes del cambio, para publicar el microservicio local sin detener la aplicación de Wondershare. No fue necesario cambiar el detector facial ni las reglas de reconocimiento.

## Configuraciones modificadas

| Archivo local | Configuración actual | Finalidad |
|---|---|---|
| `micro-facial/.env` | `SERVICE_PORT=8091` | Publicar el contenedor en un puerto de Windows sin el conflicto observado. |
| `FaceLit-Backend/.env` | `FACIAL_EMBEDDING_URL=http://127.0.0.1:8091` | Enviar las solicitudes de Java al nuevo puerto publicado. |

Se recreó únicamente el servicio Docker `embedding-service` para aplicar el cambio de publicación. Los archivos `.env` están ignorados por Git: esta configuración privada no viaja al clonar ni se incluye automáticamente en un PR. QA debe reproducir explícitamente esas dos variables si prueba este ambiente.

`micro-facial/docker-compose.yml` ya permite configurar el puerto mediante `${SERVICE_PORT:-8090}:8090`; no se modificó ese archivo. Los ejemplos y el valor por defecto del proyecto siguen usando 8090. Esta nota documenta una excepción local, no un cambio obligatorio para todos los equipos ni para producción/Hostinger.

## Conexión resultante

```text
Frontend local :8081
        ↓ HTTP y sesión del usuario
Backend local :8080
        ↓ HTTP y clave interna de servicio
Windows 127.0.0.1:8091
        ↓ publicación Docker 8091 → 8090
Microservicio dentro del contenedor :8090
```

El frontend continúa llamando al backend. El microservicio sigue escuchando en 8090 dentro de Docker. No se cambian rutas, payloads, puertos de PostgreSQL, credenciales, tablas ni arquitectura. Si Java se ejecuta dentro de otro contenedor, su URL debe ser la dirección de red interna correspondiente; `127.0.0.1:8091` es la configuración documentada aquí para Java ejecutándose en Windows.

## Pasos para QA

1. Preparar las configuraciones privadas habituales de ambos servicios. En `micro-facial/.env`, agregar o actualizar únicamente `SERVICE_PORT=8091`. En `FaceLit-Backend/.env`, establecer `FACIAL_EMBEDDING_URL=http://127.0.0.1:8091`. Mantener las claves internas coincidentes, sin publicarlas. Si 8091 está ocupado por otra aplicación, elegir un puerto libre y ajustar ambos extremos de forma consistente.
2. Desde la carpeta `micro-facial`, aplicar el puerto publicado:

```powershell
docker compose up -d --no-deps --force-recreate embedding-service
docker compose ps
```

La publicación esperada es 8091 del equipo hacia 8090 del contenedor. La base de datos y los volúmenes de modelos no se eliminan con estos comandos. Esperar la finalización del arranque/calentamiento del modelo.

3. Abrir `http://127.0.0.1:8091/health`. Debe responder:

```json
{"status":"ok","service":"FaceLit Embedding Service"}
```

La raíz `http://127.0.0.1:8091/` puede responder `{"detail":"Not Found"}`: no hay una ruta `/` definida. Eso no equivale a un fallo de `/health`.

4. Reiniciar el backend para que vuelva a leer su `.env`. En su terminal, detener la ejecución anterior con Ctrl+C y, desde `FaceLit-Backend`, ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Debe permanecer activo en 8080 sin un posterior `Application run failed`. El control de credenciales del coordinador se revisa por separado en `HU46_COORDINADOR_QA.md`; cambiar el puerto no elimina ese control.

5. Abrir el frontend local en 8081, ingresar con una cuenta de aprendiz autorizada y probar el registro facial. Comprobar en Network la petición al backend `/api/facial/embeddings/liveness-check` y, al completar los retos, `/api/facial/embeddings/from-image`. El navegador debe continuar comunicándose con el backend en 8080, no enviar la clave interna al microservicio.
6. Si se aprueba el registro, comprobar la confirmación de la interfaz y la persistencia del rostro para ese aprendiz; repetir la consulta tras recargar. Registrar fecha, rama/SHA y resultado. Para acreditar asistencia, hacer además una prueba independiente en una sesión válida con un instructor autorizado.

## Evidencia y límites de esta comprobación

| Comprobación | Resultado observado |
|---|---|
| Backend escuchando en 8080 | Confirmado durante el diagnóstico. |
| Proceso ajeno a FaceLit escuchando en 8090 | Confirmado: Wondershare; además había publicación Docker/WSL. |
| Salud dentro del contenedor | Respuesta `status: ok`. |
| Puerto 8091 libre antes de recrear | Confirmado. |
| Publicación Docker 8091 → 8090 | Confirmada después de recrear el servicio. |
| Salud desde Windows en 8091 después del arranque | Respuesta `status: ok` confirmada. |
| Registro con cámara y guardado E2E después del cambio | Pendiente de evidencia de QA. La salud no acredita por sí sola ese flujo. |
| Hostinger/producción | No se modificó ni se verificó con este ajuste local. |

Para aprobar la corrección de conectividad, QA debe adjuntar la publicación del puerto, la respuesta de `/health`, el arranque sostenido del backend con la URL nueva y el resultado de la petición facial desde la aplicación. No adjuntar archivos `.env`, claves, JWT ni imágenes biométricas a repositorios o evidencias públicas.
