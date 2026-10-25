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

La configuración se entrega para publicación en hu-39-dev y revisión mediante PR hacia dev. La ejecución verde en GitHub Actions y la revisión independiente quedan pendientes de verificar; el resultado local no las reemplaza.
