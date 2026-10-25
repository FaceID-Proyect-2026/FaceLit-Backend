# Backend documentation inventory

Scope: HU-MEJ-016 QA corrections, 2026-10-25. Owner: HU-MEJ-016 assignee for relocation and current setup; HU-MEJ-018 owner to be assigned by the team for exhaustive content review.

| Original path | Destination | Decision | Responsibility |
| --- | --- | --- | --- |
| `src/main/java/com/FaceLit/backend/academic/docs_academic/01-Modulo-Gestion-Academica-Implementacion.md` | [01-Modulo-Gestion-Academica-Implementacion.md](modules/academic/01-Modulo-Gestion-Academica-Implementacion.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/academic/docs_academic/02-Guia-Pruebas-API-Gestion-Academica.md` | [02-Guia-Pruebas-API-Gestion-Academica.md](modules/academic/02-Guia-Pruebas-API-Gestion-Academica.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/academic/docs_academic/image.png` | [image.png](modules/academic/image.png) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/1.Guía_completa_de_archivos.md` | [1.Guía_completa_de_archivos.md](modules/authentication/1.Guía_completa_de_archivos.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/2.authentication_entities.md` | [2.authentication_entities.md](modules/authentication/2.authentication_entities.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/4.HU-04_Roles_sesion_usuario.md` | [4.HU-04_Roles_sesion_usuario.md](modules/authentication/4.HU-04_Roles_sesion_usuario.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/5.Hu_password_recovery.md` | [5.Hu_password_recovery.md](modules/authentication/5.Hu_password_recovery.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/Enpoin_V2.md` | [Enpoin_V2.md](modules/authentication/Enpoin_V2.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/refactor-auth-resumen.md` | [refactor-auth-resumen.md](modules/authentication/refactor-auth-resumen.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/RF-10_Gestion_de_usuarios.md` | [RF-10_Gestion_de_usuarios.md](modules/authentication/RF-10_Gestion_de_usuarios.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| `src/main/java/com/FaceLit/backend/auth/docs_autch/Role.md` | [Role.md](modules/authentication/Role.md) | Move, preserve bytes | HU-MEJ-016; content reconciliation HU-MEJ-018 |
| README.md | [README](../README.md) | Replace inaccurate versions, fictional package tree and duplicated dependency examples with verified setup | HU-MEJ-016 |

| docs/INFORME_LIMITE_SOLICITUDES_REGISTRO_FACIAL_22-10-2026.md | [INFORME_LIMITE_SOLICITUDES_REGISTRO_FACIAL_22-10-2026.md](INFORME_LIMITE_SOLICITUDES_REGISTRO_FACIAL_22-10-2026.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/RF-5.4-restablecimiento-facial.md | [RF-5.4-restablecimiento-facial.md](RF-5.4-restablecimiento-facial.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/account-access-change-report.md | [account-access-change-report.md](account-access-change-report.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/arquitectura/Conexion a base de datos.md | [Conexion a base de datos.md](arquitectura/Conexion%20a%20base%20de%20datos.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/arquitectura/Roles y Service — Conceptos clave.md | [Roles y Service — Conceptos clave.md](arquitectura/Roles%20y%20Service%20—%20Conceptos%20clave.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/diagnostico-envio-correos.md | [diagnostico-envio-correos.md](diagnostico-envio-correos.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/inventario-notificaciones.md | [inventario-notificaciones.md](inventario-notificaciones.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/notificaciones-correcciones-y-nuevos-avisos.md | [notificaciones-correcciones-y-nuevos-avisos.md](notificaciones-correcciones-y-nuevos-avisos.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/notificaciones-implementadas.md | [notificaciones-implementadas.md](notificaciones-implementadas.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |
| docs/recordatorio-salida-correo.md | [recordatorio-salida-correo.md](recordatorio-salida-correo.md) | Retain in place; detailed claims require HU-MEJ-018 review | HU-MEJ-018 owner to be assigned |

All other existing documents under docs/ are retained in place. Notification, facial and database connection narratives remain supporting records, not new validation evidence for this cleanup. Their detailed API/configuration review is tracked as HU-MEJ-018 follow-up. No Java source, Maven Wrapper, SQL migration or deployment file was moved. The embedded historical source trees in module narratives describe their original delivery and may still mention former documentation folders.

Validation must distinguish local Maven tests, PostgreSQL startup and a real hosting deployment. Relocation does not certify all historical claims. No document is deleted solely because it is old or written in Spanish; original content is preserved for team review.
