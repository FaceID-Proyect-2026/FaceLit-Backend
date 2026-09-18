# Módulo de Gestión Académica

## 1. Objetivo

Este documento describe la primera parte implementada del módulo de Gestión Académica de FaceLit: la administración manual de programas y fichas.

El módulo está preparado para crecer posteriormente con instructores, aprendices, traslado de ficha, carga CSV y entrega de credenciales.

## 2. Alcance implementado hoy

Actualmente están implementados:

- Entidad `Program` en `academic.program`.
- Entidad `Chip` en `academic.chip`.
- Entidad `Instructor` en `academic.instructor`.
- Entidad `InstructorProgram` en `academic.instructor_program`.
- Entidad `UserChip` en `security.user_chip`.
- Entidad `ChangeHistory` en `academic.change_history`.
- Estados `ACTIVE` e `INACTIVE`.
- Tipos de instructor `SPECIFIC` y `CROSS-CUTTING`.
- CRUD manual de programas.
- CRUD manual de fichas.
- Desactivación lógica y reactivación.
- Eliminación física condicionada por dependencias.
- Registro de cambios en `academic.change_history`.
- Protección de las rutas académicas para el rol `COORDINATOR`.

Todavía no están implementados como endpoints:

- CRUD manual de instructores.
- CRUD manual de aprendices.
- Asignación de instructores a programas.
- Asignación de aprendices a fichas.
- Traslado de ficha.
- Carga y confirmación de archivos CSV.
- Entrega de credenciales.

## 3. Entidades

### 3.1 Programa

Tabla: `academic.program`

| Campo | Tipo | Descripción |
|---|---|---|
| `id_program` | UUID | Identificador generado por la base de datos/JPA. |
| `program_name` | VARCHAR(100) | Nombre único del programa. |
| `program_code` | VARCHAR(15) | Código único, entre 2 y 15 caracteres alfanuméricos. |
| `state` | VARCHAR(20) | `ACTIVE` o `INACTIVE`. |
| `deactivation_reason` | VARCHAR(200) | Motivo opcional de desactivación. |
| auditoría | varios | Fechas y usuario de creación, actualización y eliminación. |

Un programa puede tener muchas fichas y muchos instructores asociados.

### 3.2 Ficha

Tabla: `academic.chip`

En el sistema, `Chip` representa una ficha académica. No tiene nombre ni jornada.

| Campo | Tipo | Descripción |
|---|---|---|
| `id_chip` | UUID | Identificador de la ficha. |
| `id_program` | UUID | Programa al que pertenece la ficha. |
| `chip_code` | VARCHAR(20) | Código único de exactamente 7 dígitos numéricos. |
| `state` | VARCHAR(20) | `ACTIVE` o `INACTIVE`. |
| `deactivation_reason` | VARCHAR(200) | Motivo opcional de desactivación. |
| auditoría | varios | Fechas y usuario de creación, actualización y eliminación. |

Cada ficha pertenece a un solo programa mediante la relación `ManyToOne`.

### 3.3 Instructor

Tabla: `academic.instructor`

El instructor es una extensión de `security.user_app`. Su tipo puede ser:

- `SPECIFIC`: instructor específico.
- `CROSS-CUTTING`: instructor transversal.

En Java el segundo valor se llama `CROSS_CUTTING`, porque los guiones no son válidos en identificadores Java. El converter JPA lo guarda en PostgreSQL como `CROSS-CUTTING`.

### 3.4 Relación instructor-programa

Tabla: `academic.instructor_program`

Relaciona instructores con programas y evita duplicar la misma relación mediante la restricción única `uq_instructor_program`.

### 3.5 Relación aprendiz-ficha

Tabla: `security.user_chip`

Relaciona un usuario con una ficha. La base de datos tiene un índice único parcial para impedir más de una asignación activa del mismo usuario.

### 3.6 Historial de cambios

Tabla: `academic.change_history`

Registra:

- Entidad afectada.
- Identificador de la entidad.
- Campo modificado.
- Valor anterior.
- Valor nuevo.
- Acción realizada.
- Fecha y usuario que realizó el cambio.

Las acciones soportadas son `CREATE`, `UPDATE`, `DEACTIVATE`, `REACTIVATE`, `DELETE`, `CSV_LOAD`, `CSV_CONFIRM` y `CSV_CANCEL`.

## 4. DTOs de entrada

### Programa

`ProgramRequestDTO` solo recibe:

```json
{
  "programName": "Análisis y Desarrollo de Software",
  "programCode": "ADSO"
}
```

No recibe `state` ni `deactivationReason`. El estado se administra mediante las operaciones del ciclo de vida.

### Ficha

`ChipRequestDTO` recibe:

```json
{
  "idProgram": "UUID_DEL_PROGRAMA",
  "chipCode": "2825551"
}
```

`chipCode` debe contener exactamente 7 números.

Nota de implementación actual: el endpoint de creación también recibe `idProgram` en la URL (`/programs/{idProgram}/chips`) y usa ese valor para asociar la ficha. El campo `idProgram` del body se valida como obligatorio, pero el servicio actual no compara ambos valores. Para evitar inconsistencias, el frontend debe enviar el mismo UUID en la URL y en el body.

## 5. Reglas de negocio

### Creación

- Todo programa nuevo comienza en `ACTIVE`.
- Una ficha solo se puede crear dentro de un programa existente y activo.
- No se permiten nombres de programa repetidos.
- No se permiten códigos de programa repetidos.
- No se permiten códigos de ficha repetidos.

### Actualización

- `PUT` actualiza los datos descriptivos.
- El `PUT` no cambia el estado.
- El `PUT` no recibe motivo de desactivación.
- El cambio se registra como `UPDATE`.

### Desactivación

La primera llamada a `DELETE` no elimina físicamente el registro:

- Cambia `ACTIVE` a `INACTIVE`.
- Guarda el motivo recibido en `?reason=` si existe.
- Registra la acción `DEACTIVATE`.

### Reactivación

`PATCH /reactivate` cambia `INACTIVE` a `ACTIVE`, elimina el motivo de desactivación y registra `REACTIVATE`.

Una ficha no puede reactivarse si su programa está inactivo.

### Eliminación física

Si el registro ya está `INACTIVE`, una segunda llamada a `DELETE` intenta eliminarlo físicamente.

Un programa no puede eliminarse físicamente si tiene:

- Fichas asociadas.
- Instructores asociados.

Una ficha no puede eliminarse físicamente si tiene aprendices asociados.

## 6. Seguridad

Las rutas `/api/academic/**` requieren autenticación y el rol `COORDINATOR`.

La configuración utiliza:

```java
.requestMatchers("/api/academic/**").hasRole("COORDINATOR")
```

Un usuario sin token recibe una respuesta de no autenticado. Un usuario autenticado con otro rol no tiene autorización para administrar el módulo.

## 7. Estructura de código

```text
academic/
├── controller/academic/
│   ├── ProgramController.java
│   └── ChipController.java
├── dto/
│   ├── request/academic/
│   │   ├── ProgramRequestDTO.java
│   │   └── ChipRequestDTO.java
│   └── response/academic/
│       ├── ProgramResponseDTO.java
│       └── ChipResponseDTO.java
├── exception/
│   └── AcademicException.java
├── model/
│   ├── academic/
│   ├── converter/
│   └── enums/
├── repository/
└── service/
    ├── academic/
    └── serviceImpl/academic/
```

## 8. Validación realizada

- Compilación Maven exitosa.
- Tests existentes ejecutados correctamente.
- Mapeos JPA cargados contra PostgreSQL.
- No se encontraron errores del analizador Java en el módulo académico.
