# Módulo de Gestión Académica

## 1. Objetivo

Este documento describe la implementación del módulo académico de FaceLit, con el alcance actual de programas, fichas, instructores, asignación de aprendices a fichas y traslado de ficha.

La lógica del negocio se orienta a la gestión coordinada de:

- programas académicos
- fichas por programa
- instructores con tipos `ESPECIFICO` y `TRANSVERSAL`
- relación aprendiz-ficha
- historial de cambios por entidad

## 2. Alcance implementado

Actualmente están implementados los siguientes componentes:

- Entidad `Program` en `academic.program`
- Entidad `Chip` en `academic.chip`
- Entidad `Instructor` en `academic.instructor`
- Entidad `InstructorProgram` en `academic.instructor_program`
- Entidad `UserChip` en `security.user_chip`
- Entidad `ChangeHistory` en `academic.change_history`
- Estados `ACTIVE` e `INACTIVE`
- Tipos de instructor `ESPECIFICO` y `TRANSVERSAL`
- CRUD de programas
- CRUD de fichas
- CRUD de instructores
- Vinculación de instructores a programas
- Asignación inicial de un aprendiz a una ficha
- Traslado de ficha entre fichas activas
- Búsquedas académicas por documento, nombre, tipo, programa elegible y código
- Registro de cambios en `academic.change_history`
- Protección de rutas `/api/academic/**` para rol `COORDINATOR`

## 3. Entidades del negocio

### 3.1 Programa

Tabla: `academic.program`

| Campo | Tipo | Descripción |
|---|---|---|
| `id_program` | UUID | Identificador generado por JPA. |
| `program_name` | VARCHAR(100) | Nombre único del programa. |
| `program_code` | VARCHAR(15) | Código único del programa. |
| `state` | VARCHAR(20) | `ACTIVE` / `INACTIVE`. |
| `deactivation_reason` | VARCHAR(200) | Motivo de desactivación. |
| auditoría | varios | Fechas y auditoría del sistema. |

### 3.2 Ficha

Tabla: `academic.chip`

| Campo | Tipo | Descripción |
|---|---|---|
| `id_chip` | UUID | Identificador de la ficha. |
| `id_program` | UUID | Programa asociado. |
| `chip_code` | VARCHAR(20) | Código único de la ficha. |
| `state` | VARCHAR(20) | `ACTIVE` / `INACTIVE`. |
| `deactivation_reason` | VARCHAR(200) | Motivo de desactivación. |
| auditoría | varios | Fechas y auditoría del sistema. |

### 3.3 Instructor

Tabla: `academic.instructor`

El instructor no tiene ciclo de vida propio en esta tabla. La cuenta del usuario sigue viva en `security.user_app`, y el estado real de la cuenta del instructor se maneja desde el Módulo 1 de usuarios.

Reglas importantes:

- No se implementan `state` ni `deactivation_reason` en `academic.instructor`.
- Un usuario solo puede existir una vez como instructor.
- El `instructor_type` puede ser:
  - `ESPECIFICO`
  - `TRANSVERSAL`

En Java se usan los nombres de negocio `ESPECIFICO` y `TRANSVERSAL`. El
converter JPA los persiste en PostgreSQL con los valores definidos por la
restricción de la tabla: `SPECIFIC` y `CROSS-CUTTING`.

### 3.4 Relación instructor-programa

Tabla: `academic.instructor_program`

- `ESPECIFICO`: tiene una o varias filas según los programas asignados.
- `TRANSVERSAL`: no tiene filas en esta tabla.
- La restricción única evita duplicados por instrucción/programa.

### 3.5 Relación aprendiz-ficha

Tabla: `security.user_chip`

- Un usuario puede tener solo una ficha activa a la vez.
- La relación no se elimina en el traslado: la fila antigua queda `INACTIVE` y se crea una nueva `ACTIVE`.
- Esto permite mantener histórico de fichas del aprendiz.

### 3.6 Historial de cambios

Tabla: `academic.change_history`

Se registran cambios en:

- `instructor`
- `instructor_program`
- `user_chip`
- `chip`
- `program`

Acciones registradas:

- `CREATE`
- `UPDATE`
- `DELETE`
- `DEACTIVATE`
- `REACTIVATE`

## 4. Reglas de negocio importantes

### 4.1 Instructor

- `POST /api/academic/instructors`
  - valida que el usuario exista
  - valida que el usuario no esté ya registrado como instructor
  - si es `ESPECIFICO`, exige al menos un `programIds`
  - crea las relaciones en `instructor_program`
- `PUT /api/academic/instructors/{id}`
  - reemplaza la lista completa de programas del instructor
  - si cambia a `TRANSVERSAL`, elimina todas las filas relacionadas
- `DELETE /api/academic/instructors/{id}`
  - elimina primero las relaciones `instructor_program`
  - deja preparado el control por asistencias como TODO estructural, sin romper el flujo

### 4.2 Asignación inicial de ficha

- `POST /api/academic/chips/{idChip}/apprentices`
- Validaciones:
  - la ficha debe existir
  - la ficha debe estar `ACTIVE`
  - el usuario debe existir
  - el usuario no puede tener otra ficha activa
- Se crea una fila en `security.user_chip` con `state = ACTIVE` y `assignment_date = now`.

### 4.3 Traslado de ficha

- `GET /api/academic/users/{idUser}/chip/transfer-targets`
  - devuelve las fichas activas del sistema, excluyendo la actual
- `POST /api/academic/users/{idUser}/chip/transfer`
  - requiere el `idNewChip` en el body
  - la operación debe conservar exactamente una ficha activa para el usuario
  - la fila actual se desactiva y la nueva queda `ACTIVE`

Este comportamiento es el punto clave del módulo: no se permite dejar al aprendiz sin ficha activa ni con dos fichas activas a la vez.

### 4.4 Búsquedas

Se implementaron búsquedas del tipo:

- `GET /api/academic/programs/search?name=...`
- `GET /api/academic/programs/code/{code}`
- `GET /api/academic/chips/search?code=...`
- `GET /api/academic/instructors/search?document=&name=&type=`
- `GET /api/academic/users/{idUser}/chip`
- `GET /api/academic/users/{idUser}/chip/history`
- `GET /api/academic/change-history?entityName=&entityId=`

Las búsquedas por texto son insensibles a mayúsculas y usan coincidencia parcial salvo el código exacto.

## 5. Endpoints implementados

### 5.1 Programas

- `POST /api/academic/programs`
- `GET /api/academic/programs`
- `GET /api/academic/programs/{idProgram}`
- `PUT /api/academic/programs/{idProgram}`
- `PATCH /api/academic/programs/{idProgram}/reactivate`
- `DELETE /api/academic/programs/{idProgram}`

### 5.2 Fichas

- `POST /api/academic/programs/{idProgram}/chips`
- `GET /api/academic/programs/{idProgram}/chips`
- `GET /api/academic/chips/{idChip}`
- `PUT /api/academic/chips/{idChip}`
- `PATCH /api/academic/chips/{idChip}/reactivate`
- `DELETE /api/academic/chips/{idChip}`

### 5.3 Instructores

- `POST /api/academic/instructors`
- `PUT /api/academic/instructors/{idInstructor}`
- `DELETE /api/academic/instructors/{idInstructor}`
- `GET /api/academic/instructors`
- `GET /api/academic/instructors/{idInstructor}`
- `GET /api/academic/instructors/user/{idUser}`
- `GET /api/academic/instructors/search?document=&name=&type=`
- `GET /api/academic/instructors/eligible?idProgram={id}`

### 5.4 Aprendices y fichas

- `POST /api/academic/chips/{idChip}/apprentices`
- `GET /api/academic/chips/{idChip}/apprentices`
- `GET /api/academic/users/{idUser}/chip`
- `GET /api/academic/users/{idUser}/chip/history`
- `GET /api/academic/users/{idUser}/chip/transfer-targets`
- `POST /api/academic/users/{idUser}/chip/transfer`

### 5.5 Carga institucional CSV

- `GET /api/academic/csv/template`
- `POST /api/academic/csv/upload` con `multipart/form-data` y parte `file`
- `GET /api/academic/csv/pending-transfers`
- `POST /api/academic/csv/pending-transfers/{id}/accept`
- `POST /api/academic/csv/pending-transfers/{id}/cancel`

La carga se procesa en memoria y por fases: `programa`, `ficha`, `instructor` y
`aprendiz`. Cada fila conserva su número original y los errores de una fila se
devuelven en el resumen sin detener las demás filas. El archivo debe ser CSV,
UTF-8, pesar como máximo 5 MB y tener como máximo 5000 filas de datos.

La tabla `academic.csv_pending_transfer` conserva las propuestas de traslado
detectadas por CSV. Una propuesta pendiente no mueve al aprendiz hasta que el
Coordinador ejecuta `accept`. `cancel` conserva el registro como historial y
no modifica la ficha activa.

## 6. Seguridad

Las rutas del módulo académico se protegen con:

```java
.requestMatchers("/api/academic/**").hasRole("COORDINATOR")
```

Esto significa que solo un usuario autenticado con rol `ROLE_COORDINATOR` puede acceder a cada endpoint del módulo.

## 7. Estructura del código

```text
academic/
├── controller/academic/
│   ├── ProgramController.java
│   ├── ChipController.java
│   ├── InstructorController.java
│   └── UserChipController.java
│   └── CsvAcademicController.java
├── dto/
│   ├── request/academic/
│   │   ├── ProgramRequestDTO.java
│   │   ├── ChipRequestDTO.java
│   │   ├── InstructorRequestDTO.java
│   │   ├── UserChipRequestDTO.java
│   │   └── TransferChipRequestDTO.java
│   └── response/academic/
│       ├── ProgramResponseDTO.java
│       ├── ChipResponseDTO.java
│       ├── InstructorResponseDTO.java
│       ├── UserChipResponseDTO.java
│       ├── CsvUploadResponseDTO.java
│       └── PendingTransferResponseDTO.java
├── exception/
│   └── AcademicException.java
├── model/
│   ├── academic/
│   ├── converter/
│   └── enums/
├── repository/
├── service/
│   ├── academic/
│   └── serviceImpl/academic/
└── docs_academic/
```

La entidad nueva es `CsvPendingTransfer`, con estados `PENDING`, `ACCEPTED` y
`CANCELLED`. La tabla correspondiente se administra en el esquema `academic`
desde el repositorio de base de datos.

## 8. Validación realizada

La compilación del proyecto se validó ejecutando:

```bash
mvn test -q
```

Resultado: compilación exitosa.

## 9. Observaciones de diseño

- La tabla `academic.instructor` no lleva `state`; eso se maneja en `security.user_app`.
- La eliminación de instructor está preparada para la guarda estructural por asistencia, aunque aún no existe el módulo de asistencia real.
- El traslado de ficha se debe mantener atómico para evitar ventanas donde el aprendiz quede sin ficha activa.
- El coordinador es el único actor del flujo de traslado y asignación; el aprendiz no ingresa códigos ni confirma en ningún punto.
