# HU-05 / HU-MEJ-005 — Pasarela biométrica autenticada

Responsable: **Mariana Valenzuela Penagos**. Rama: `hu-mej-005`.
Hallazgos: **H-03, H-15, H-48**. Plan: **punto 4**.

## Contrato

| Ruta del backend | Permiso | Destino interno |
| --- | --- | --- |
| `POST /api/facial/embeddings/from-image` | JWT y rol APPRENTICE; dueño tomado del principal | `/api/v1/facial-embeddings/from-image` |
| `POST /api/facial/embeddings/liveness-check` | JWT y rol APPRENTICE o INSTRUCTOR | `/api/v1/facial-embeddings/liveness-check` |

El registro conserva `reset_token`, `replace_existing`, imágenes y retos; nunca
confía en un `user_id` o `created_by` suministrado por el cliente. La autorización
previa del reemplazo continúa aplicándose en micro-facial. No hay rutas de pasarela
para lectura, listado, PATCH o DELETE arbitrarios. Se filtran las respuestas por una
lista de campos permitidos, sin vectores. Los errores de credencial interna son 502
para evitar cerrar una sesión JWT válida en el móvil.

## Configuración obligatoria

Definir `FACIAL_EMBEDDING_API_KEY` en el entorno privado del backend y el mismo
secreto en `API_KEY` de micro-facial. No hay valor de respaldo. La construcción del
cliente falla ante clave ausente, vacía o con espacios. `FACIAL_EMBEDDING_URL`
apunta a localhost cuando el backend corre en el mismo equipo, o al nombre privado
del contenedor cuando comparten red. Reiniciar ambos servicios al rotar.

Se generó un secreto aleatorio de 48 bytes, sincronizado solo en los archivos
locales ignorados. Su valor no se publica en pruebas ni documentación. El operador
debe rotar también cualquier secreto desplegado fuera de este equipo.

## Pruebas

Las pruebas de pasarela cubren identidad, roles, acceso anónimo, filtro de vectores,
conservación de autorización y errores internos. La prueba HTTP del cliente verifica
la credencial de servicio en la cabecera. Se ejecutó también regresión de registro,
autorización de reemplazo, período de espera y notificaciones de asistencia.

El informe detalla resultados y la exclusión temporal de una prueba previa sin
seguimiento de HU-002: [INFORME-HU-MEJ-005.md](INFORME-HU-MEJ-005.md).
