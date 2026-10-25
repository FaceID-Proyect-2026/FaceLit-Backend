# HU-MEJ-006 — CI del backend

## Alcance

Configuración en la rama hu-39-dev, basada en 626d25f962edb861f5b0d3964ce6e2953382cec4. No se modifican lógica Java, endpoints, contratos, pruebas existentes ni migraciones. Las eliminaciones locales de otros documentos y los cambios de caché .m2 son ajenos a esta HU.

## Qué ejecuta GitHub Actions

Archivo: .github/workflows/ci.yml. Se ejecuta en todos los PR y en pushes a dev, qa y main. También permite ejecución manual cuando el workflow esté disponible en la rama correspondiente.

1. Preparar Java 17 y caché Maven.
2. Habilitar el wrapper en Linux.
3. Ejecutar prepare-agent de JaCoCo 0.8.12, Maven verify y el reporte de JaCoCo, sin omitir pruebas.
4. Subir Surefire, Failsafe si existe, y el informe de cobertura como artifact backend-tests-and-coverage.

El perfil test utiliza H2 y deshabilita Liquibase conforme a la configuración de pruebas existente. JWT y correo reciben valores ficticios de CI. No se usan secrets productivos; el host SMTP de pruebas es localhost. No se afirma que esto valide PostgreSQL real ni entrega de correo.

## Qué es JaCoCo

JaCoCo mide qué líneas y ramas del código Java fueron ejecutadas por las pruebas. No crea pruebas ni garantiza que sus aserciones sean correctas. El informe HTML se encuentra en target/site/jacoco/index.html dentro del artifact. También se conserva XML para integrar herramientas de cobertura en el futuro.

No se establece un porcentaje mínimo arbitrario. La prueba fallida hace fallar CI; un porcentaje bajo por sí solo no bloquea hasta que el equipo acuerde y configure un umbral.

Referencia oficial: https://www.jacoco.org/jacoco/trunk/doc/maven.html

## Protección y evidencia

Después de publicar y ejecutar el PR, exigir el check Maven tests and JaCoCo coverage en las ramas de integración mediante el ruleset. Exigir PR y aprobación independiente. El archivo YAML no configura por sí solo las protecciones.

Guardar SHA, URL de PR, URL del run y artifact. La HU no está Done sin CI verde real y revisión independiente. H-17 necesita los casos de autorización pendientes; esta configuración solo ejecuta los existentes.

## Estado de validación

- YAML parseado correctamente y git diff --check aprobado.
- Maven verify con JaCoCo 0.8.12: BUILD SUCCESS, salida 0.
- 123 pruebas ejecutadas; 0 fallos, 0 errores, 0 omitidas.
- Cobertura de líneas JaCoCo: 62,72 % (2857 cubiertas, 1698 sin cubrir).
- Informe local: target/site/jacoco/index.html y target/site/jacoco/jacoco.xml. Evidencia de pruebas en target/surefire-reports; log de esta ejecución en ../hu006-backend-ci.log. No agregar target, cachés ni logs completos al commit.
- La validación local utilizó el JDK 21 instalado y compilación release 17. El workflow configura Java 17; su ejecución real en GitHub Actions todavía debe verificarse.

## Actualización de evidencia y alcance — 25 de octubre de 2026

Rama de esta entrega: `hu-39-dev`, PR [#67 hacia dev](https://github.com/FaceID-Proyect-2026/FaceLit-Backend/pull/67). El SHA base `0e6666b3f726746b8026df8eb83b7941bf7849fa` ya tiene una [ejecución real verde](https://github.com/FaceID-Proyect-2026/FaceLit-Backend/actions/runs/40195794058). Esa evidencia corresponde a ese SHA; al publicar esta actualización documental debe revisarse el CI del nuevo head del PR.

Se repitió Maven verify y JaCoCo sobre una copia del código versionado de ese SHA: **123 pruebas aprobadas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS**. Cobertura de líneas: **62,72 % (2857/4555)**. Perfil test/H2, Maven 3.9.14 y JDK 21.0.7 compilando para Java 17; el workflow usa JDK 17. No se incluyeron pruebas sin seguimiento, cachés ni borrados locales. La clase opt-in `FacialRegistrationConfirmationPostgresIT` no forma parte de las 123 pruebas ejecutadas.

El primer intento local encontró un bloqueo de comunicación Mockito/JVM dentro del sandbox; se repitió la misma validación fuera de esa restricción. Las advertencias iniciales H2 de drop de un esquema aún inexistente no impidieron el resultado exitoso. Los logs completos permanecen fuera del repositorio, en `ci-evidence/hu006-validation-20261025/` del directorio de trabajo.

La delimitación solicitada por la responsable asigna HU-MEJ-006 a **H-16/H-18: automatización, cobertura y evidencia**. La propuesta inicial citaba también H-17; su implementación de seguridad se deriva a HU-MEJ-001 (autorización/pertenencia, 401/403) y HU-MEJ-002 (recuperación, intentos y JWT invalidado). H-17 es un hallazgo, no HU-MEJ-017. CI debe ejecutar sus pruebas cuando se integren; este PR no afirma que esas reglas estén implementadas ni cierra H-17.

Se configuró como obligatorio `Maven tests and JaCoCo coverage` en el ruleset CI activo de dev/qa/main, conservando una aprobación y sin bypass. La responsable informa haber probado el bloqueo con CI rojo: falta adjuntar y contrastar esa evidencia. La última consulta de revisiones no tenía aprobaciones registradas; debe comprobarse su estado final en el PR. No se declara Done antes de evidencia y aprobación independiente.

La cobertura indica líneas recorridas, no porcentaje de funcionamiento ni garantía de ausencia de defectos. La HU no define un umbral mínimo del 100 %. No se modificaron código de negocio ni workflow durante esta actualización documental.

Documento de entrega compartido: [alcance, inventario y resultados de HU-MEJ-006](https://github.com/FaceID-Proyect-2026/FaceLit/blob/hu-38-dev/docs/HU-MEJ-006-ALCANCE-Y-ENTREGA-QA.md).
