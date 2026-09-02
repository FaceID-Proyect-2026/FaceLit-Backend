# Refactor Backend — Resumen de Cambios

**Fecha:** 2026-08-08
**Estado general:** 🟡 En progreso — módulo `auth` completado, `environments`/`academic` completado

---

# Parte 1 — Módulo `auth`

**Alcance:** Módulo `auth` (security, legal, roleandpermission)
**Riesgo:** Bajo — ningún cambio afecta DTOs, endpoints ni contratos con el frontend
**Estado:** ✅ Completado

---

## 1. Problema detectado

Al auditar el módulo `auth` (ya conectado y funcionando con el frontend) se encontraron dos malas prácticas de la tabla de diagnóstico, presentes **dentro del propio módulo auth**, no solo en los módulos pendientes:

| Problema | Dónde aparecía |
|---|---|
| **Copy & Paste** | Generación de código de 6 dígitos duplicada en 3 clases. Cálculo de `isMinor`/edad duplicado en 4 puntos. Cooldown de reenvío (60s) duplicado en 2 clases. |
| **Magic Numbers** | `999999`, `60`, `18`, `8`, `100` hardcodeados en varios servicios, mientras que `RegisterServiceImpl` ya usaba `AppConstants` para lo mismo — inconsistencia. |

---

## 2. Archivos nuevos

### `shared/constants/AppConstants.java` (ampliado)
Se agregaron las constantes que faltaban:
```java
public static final int RESEND_COOLDOWN_SECONDS = 60;
public static final int MIN_AGE = 8;
public static final int MAX_AGE = 100;
public static final int LEGAL_AGE = 18;
```
Más un constructor privado para evitar instanciación (buena práctica en clases de solo constantes).

### `shared/util/VerificationCodeGenerator.java` (nuevo, `@Component`)
Centraliza la generación del código de verificación de 6 dígitos. Reemplaza las 3 implementaciones repetidas de `new Random().nextInt(...)` + `String.format(...)`.

### `shared/util/AgeUtils.java` (nuevo, clase utilitaria estática)
Centraliza:
- `calculateAge(LocalDate birthDate)`
- `isMinor(LocalDate birthDate, String documentAbbreviation)`

Reemplaza el cálculo repetido de edad/minoría de edad que vivía duplicado en `RegisterServiceImpl` (3 veces) y `checkStatus()`.

---

## 3. Archivos modificados

### `RegisterServiceImpl`
- Constructor: se agregó `VerificationCodeGenerator` como dependencia inyectada.
- `register()`:
  - `age` ahora sale de `AgeUtils.calculateAge(dto.getBirthDate())` (se mantiene la variable porque se usa en las validaciones TI/CC antes de crear el `User`).
  - Validaciones TI/CC usan `AppConstants.LEGAL_AGE` en vez de `18` hardcodeado.
  - `isMinor` ahora sale de `AgeUtils.isMinor(dto.getBirthDate(), abbreviation)`.
  - Generación de código: `verificationCodeGenerator.generate()` en vez de `Random` + `String.format` inline.
- `emailVerification()`: `isMinor` ahora sale de `AgeUtils.isMinor(user.getBirthDate(), abbreviation)` (aquí ya existe el `User`, así que se le pasa directo).
- `resendCode()`: `60` hardcodeado → `AppConstants.RESEND_COOLDOWN_SECONDS`.
- `checkStatus()`: mismo fix — `age` + `isMinor` manual reemplazado por `AgeUtils.isMinor(user.getBirthDate(), abbreviation)`. Ya no necesita la variable `age` suelta, así que se pudo eliminar junto con el import de `Period` (verificar que no se use en otro punto de la clase antes de borrar el import).

### `PasswordRecoveryServiceImpl`
- Constructor: se agregó `VerificationCodeGenerator` como dependencia inyectada.
- `requestRecovery()`: `new Random().nextInt(999999)` → `verificationCodeGenerator.generate()`.

### `ConsentServiceImpl`
- Constructor: se agregó `VerificationCodeGenerator` como dependencia inyectada.
- `requestConsent()` y `resendConsentRequest()`: `new Random().nextInt(999999)` → `verificationCodeGenerator.generate()`.
- `resendConsentRequest()`: `60` hardcodeado → `AppConstants.RESEND_COOLDOWN_SECONDS`.

### `UserConfigurationController`
- `create()`, `update()`, `get()`: el parámetro `@AuthenticationPrincipal Object principal` + `UUID.fromString(principal.toString())` se simplificó a `@AuthenticationPrincipal UUID userId` directo.
- Motivo: el `JwtFilter` ya construye la autenticación con el `UUID` como principal (`jwtService.extractUserId(token)`), así que el viaje `UUID → String → UUID` era innecesario.

---

## 4. Por qué esto no rompe nada conectado al frontend

- Ningún DTO de request/response cambió de forma.
- Ninguna ruta (`@PostMapping`, `@GetMapping`, etc.) cambió.
- El header `Authorization: Bearer ...` se sigue leyendo igual.
- Los cambios son 100% internos: de dónde sale un número o una constante, no qué se expone hacia afuera.

---

## 5. Pendiente al cierre de esta parte

Estos puntos de tu tabla de diagnóstico **no se tocaron en la Parte 1** porque pertenecían a módulos aún no compartidos en ese momento:

| Patrón/Problema | Prioridad | Módulo |
|---|---|---|
| Strategy (validaciones de excepción) | Alta | Por definir |
| Observer (notificaciones RF-8) | Alta | notificaciones |
| Command (bitácora) | Media | admin/auditoría |
| God Object — `ProgramServiceImpl` | Alta | académico → **resuelto en Parte 2** |
| Copy & Paste — eliminación en cascada | Alta | académico/environments → **resuelto en Parte 2** |

---

# Parte 2 — Módulos `environments` y `academic`

**Alcance:** RF-2 (Gestión de Ambientes) y RF-3 (Gestión Académica: programas, fichas, aprendices)
**Riesgo:** Bajo — ningún cambio afecta DTOs, endpoints ni contratos con el frontend
**Estado:** ✅ Completado

## 1. Problemas detectados

| Problema | Dónde aparecía |
|---|---|
| **Dependencias muertas** | `ChipServiceImpl` inyectaba `ScheduleExceptionRepository`, `ScheduleInstructorRepository` y `RecordEnvironmentRepository` en el constructor sin asignarlos ni usarlos nunca. |
| **Magic Number** | `generateUniqueCode()` en `ChipServiceImpl` usaba `substring(0, 8)` hardcodeado, ignorando el `AppConstants.CHIP_CODE_LENGTH` que ya existía. |
| **God Object** (tu tabla) | `ProgramServiceImpl` cargaba con lógica de validación de integridad referencial (verificar dependientes antes de borrar) que no es responsabilidad pura de CRUD de programas. |
| **Copy & Paste** (tu tabla) | `permanentDeleteChip`, `permanentDeleteProgram` y `permanentDeleteEnvironment` repetían el mismo patrón: verificar estado inactivo → verificar listas de dependientes vacías → `deleteById`. Este era el mismo hallazgo visto desde dos ángulos distintos de la tabla. |
| **Antipatrón `principal`** | `UserChipController.joinChip()` y `.myChips()` repetían `UUID.fromString(principal.toString())`, igual que el caso ya resuelto en `UserConfigurationController` (Parte 1). |

## 2. Archivo nuevo

### `shared/util/DeletionGuard.java`
Utilidad estática que centraliza la validación "no eliminar si tiene dependientes". No usa herencia ni genéricos con `List<T>` porque cada módulo lanza una excepción distinta (`ChipException`, `ProgramException`, `EnvironmentException`) — en su lugar, recibe el conteo, el mensaje y el constructor de la excepción como parámetros (`Function<String, RuntimeException>`), aprovechando que las excepciones ya seguían un constructor `(String message)` consistente.

```java
public static void assertNoDependents(
        long dependentCount,
        String dependentLabel,   // ya en su forma final, ej: "ficha(s)", "aprendiz(es)"
        String actionHint,
        Function<String, RuntimeException> exceptionFactory)
```

Decisión de diseño: el label de plural **no se genera automáticamente** — se pasa tal cual se quiere mostrar, para no forzar reglas gramaticales del español dentro de una utilidad genérica.

## 3. Métodos `countBy...` agregados a los repositorios

Antes se traía la lista completa de entidades solo para verificar `.isEmpty()` o `.size()`. Se agregaron equivalentes `count` (más eficientes, no cargan entidades a memoria):

- `ChipRepository.countByProgram_IdProgram(UUID)`
- `UserChipRepository.countByChip_IdChip(UUID)`
- `ChipEnvironmentRepository.countByChip_IdChip(UUID)` y `.countByEnvironment_IdEnvironment(UUID)`
- `ScheduleRepository.countByChip_IdChip(UUID)`
- `RecordEnvironmentRepository.countAllByEnvironment_IdEnvironment(UUID)`

## 4. Archivos modificados

### `ProgramServiceImpl`
- `permanentDeleteProgram()`: el bloque manual de verificación de fichas asociadas se reemplazó por una llamada a `DeletionGuard.assertNoDependents(...)`.

### `EnvironmentServiceImpl`
- `permanentDeleteEnvironment()`: los 2 bloques manuales (fichas asignadas, horarios) se reemplazaron por 2 llamadas a `DeletionGuard.assertNoDependents(...)`.

### `ChipServiceImpl`
- `permanentDeleteChip()`: los 3 bloques manuales (aprendices, ambientes, horarios) se reemplazaron por 3 llamadas a `DeletionGuard.assertNoDependents(...)`.
- Constructor: se eliminaron los 3 parámetros muertos (`ScheduleExceptionRepository`, `ScheduleInstructorRepository`, `RecordEnvironmentRepository`) y sus imports correspondientes.
- `generateUniqueCode()`: `substring(0, 8)` → `substring(0, AppConstants.CHIP_CODE_LENGTH)`.

### `UserChipController`
- `joinChip()` y `myChips()`: `@AuthenticationPrincipal Object principal` + `UUID.fromString(principal.toString())` → `@AuthenticationPrincipal UUID userId` directo. Se reemplazó el fully-qualified name inline por un import normal de `AuthenticationPrincipal`.

## 5. Por qué esto no rompe nada conectado al frontend

- Ningún DTO de request/response cambió de forma.
- Ninguna ruta cambió.
- Los mensajes de error que ve el usuario final son textualmente los mismos (mismo texto, misma estructura) — solo cambió cómo se construyen internamente.
- Los parámetros eliminados del constructor de `ChipServiceImpl` nunca se usaban, así que ningún comportamiento visible dependía de ellos.

## 6. Pendiente

| Patrón/Problema | Prioridad | Módulo |
|---|---|---|
| Strategy (validaciones de excepción) | Alta | Por definir |
| Observer (notificaciones RF-8) | Alta | notificaciones |
| Command (bitácora) | Media | admin/auditoría |

Cuando compartas el módulo de horarios/notificaciones seguimos con el mismo enfoque: identificar el problema puntual antes de tocar código, y aplicar el cambio mínimo necesario sin afectar lo que ya está conectado.