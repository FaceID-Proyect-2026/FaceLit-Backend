# Guía de pruebas API - Gestión Académica

## 1. Preparación

### Servidor

El backend corre normalmente en:

```text
http://localhost:8080
```

### Autenticación

Las rutas académicas requieren un JWT de un usuario con rol `COORDINATOR`.

En todos los ejemplos protegidos se debe agregar:

```http
Authorization: Bearer TU_JWT_DE_COORDINATOR
Content-Type: application/json
```

Si el JWT no tiene el rol correcto, la prueba no llegará al controlador académico.

## 2. Flujo recomendado de prueba

Ejecuta las pruebas en este orden:

1. Crear un programa.
2. Guardar el `idProgram` de la respuesta.
3. Consultar todos los programas.
4. Consultar el programa por ID.
5. Editar el programa.
6. Crear una ficha dentro del programa.
7. Guardar el `idChip` de la respuesta.
8. Consultar las fichas del programa.
9. Consultar la ficha por ID.
10. Editar la ficha.
11. Desactivar la ficha.
12. Reactivar la ficha.
13. Desactivar el programa y verificar que no pueda eliminarse físicamente mientras tenga ficha.

## 3. Programas

### 3.1 Crear programa

```http
POST http://localhost:8080/api/academic/programs
```

Body válido:

```json
{
  "programName": "Analisis y Desarrollo de Software",
  "programCode": "ADSO"
}
```

Respuesta esperada: `200 OK`

```json
{
  "idProgram": "UUID_GENERADO",
  "programName": "Analisis y Desarrollo de Software",
  "programCode": "ADSO",
  "state": "ACTIVE",
  "deactivationReason": null
}
```

Guarda el UUID como `ID_PROGRAM`.

### 3.2 Listar programas

```http
GET http://localhost:8080/api/academic/programs
```

Respuesta esperada: `200 OK` con una lista de programas.

### 3.3 Consultar un programa

```http
GET http://localhost:8080/api/academic/programs/ID_PROGRAM
```

### 3.4 Editar un programa

```http
PUT http://localhost:8080/api/academic/programs/ID_PROGRAM
```

Body:

```json
{
  "programName": "Analisis y Desarrollo de Software - Actualizado",
  "programCode": "ADSO2"
}
```

El estado debe conservarse. Esta operación se registra como `UPDATE`.

### 3.5 Reactivar un programa

```http
PATCH http://localhost:8080/api/academic/programs/ID_PROGRAM/reactivate
```

Solo funciona si el programa está `INACTIVE`.

### 3.6 Desactivar un programa

```http
DELETE http://localhost:8080/api/academic/programs/ID_PROGRAM?reason=Programa%20cerrado
```

Primera llamada esperada:

- `200 OK`.
- El programa pasa a `INACTIVE`.
- Se guarda `deactivationReason`.
- Se registra la acción `DEACTIVATE`.

Importante: aunque la ruta HTTP sea `DELETE`, la primera llamada es una desactivación lógica.

### 3.7 Eliminar físicamente un programa

Repite:

```http
DELETE http://localhost:8080/api/academic/programs/ID_PROGRAM
```

Si no tiene fichas ni instructores asociados, se elimina físicamente.

Si tiene dependencias, se espera `400 Bad Request`:

```json
{
  "message": "No se puede eliminar el programa porque tiene fichas o instructores asociados"
}
```

## 4. Fichas

### 4.1 Crear ficha

La ruta recibe el programa al que pertenece:

```http
POST http://localhost:8080/api/academic/programs/ID_PROGRAM/chips
```

Body:

```json
{
  "idProgram": "ID_PROGRAM",
  "chipCode": "2825551"
}
```

Respuesta esperada: `200 OK`

```json
{
  "idChip": "UUID_GENERADO",
  "idProgram": "ID_PROGRAM",
  "chipCode": "2825551",
  "state": "ACTIVE",
  "deactivationReason": null
}
```

Guarda el UUID como `ID_CHIP`.

Nota: el `idProgram` del body debe ser igual al `ID_PROGRAM` de la URL. Actualmente el servicio usa el ID de la URL para establecer la relación, pero el DTO exige que el campo también esté presente.

### 4.2 Listar fichas de un programa

```http
GET http://localhost:8080/api/academic/programs/ID_PROGRAM/chips
```

### 4.3 Consultar una ficha

```http
GET http://localhost:8080/api/academic/chips/ID_CHIP
```

### 4.4 Editar una ficha

```http
PUT http://localhost:8080/api/academic/chips/ID_CHIP
```

Body:

```json
{
  "idProgram": "ID_PROGRAM",
  "chipCode": "2825552"
}
```

El estado no se modifica con `PUT`.

### 4.5 Desactivar una ficha

```http
DELETE http://localhost:8080/api/academic/chips/ID_CHIP?reason=Ficha%20cerrada
```

Primera llamada esperada:

- `200 OK`.
- La ficha pasa a `INACTIVE`.
- Se guarda el motivo.
- Se registra `DEACTIVATE`.

### 4.6 Reactivar una ficha

```http
PATCH http://localhost:8080/api/academic/chips/ID_CHIP/reactivate
```

La ficha solo puede reactivarse si su programa está `ACTIVE`.

### 4.7 Eliminar físicamente una ficha

Repite:

```http
DELETE http://localhost:8080/api/academic/chips/ID_CHIP
```

Si no tiene aprendices asociados, se elimina físicamente.

Si tiene aprendices asociados, se espera:

```json
{
  "message": "No se puede eliminar la ficha porque tiene aprendices asociados"
}
```

## 5. Pruebas de validación de programa

### Nombre vacío

```json
{
  "programName": "",
  "programCode": "ADSO"
}
```

Respuesta esperada: `400 Bad Request`.

```json
{
  "programName": "El nombre del programa es obligatorio"
}
```

### Código vacío

```json
{
  "programName": "Programa de Prueba",
  "programCode": ""
}
```

Respuesta esperada:

```json
{
  "programCode": "El código de programa es obligatorio"
}
```

### Código con espacios o símbolos

```json
{
  "programName": "Programa de Prueba",
  "programCode": "AD-SO"
}
```

Respuesta esperada:

```json
{
  "programCode": "El código de programa solo puede contener letras y números, sin espacios, entre 2 y 15 caracteres"
}
```

### Código demasiado corto

```json
{
  "programName": "Programa de Prueba",
  "programCode": "A"
}
```

Debe responder `400 Bad Request`.

### Código duplicado

Crea dos programas con el mismo `programCode`.

Respuesta esperada:

```json
{
  "message": "Ya existe un programa con ese código"
}
```

### Nombre duplicado

Crea dos programas con el mismo `programName`.

Respuesta esperada:

```json
{
  "message": "Ya existe un programa con ese nombre"
}
```

## 6. Pruebas de validación de ficha

### Código vacío

```json
{
  "idProgram": "ID_PROGRAM",
  "chipCode": ""
}
```

Respuesta esperada:

```json
{
  "chipCode": "El código de la ficha es obligatorio"
}
```

### Código con letras

```json
{
  "idProgram": "ID_PROGRAM",
  "chipCode": "28255A1"
}
```

Respuesta esperada:

```json
{
  "chipCode": "El código de la ficha debe tener 7 dígitos numéricos"
}
```

### Código con menos de 7 dígitos

```json
{
  "idProgram": "ID_PROGRAM",
  "chipCode": "282555"
}
```

Debe responder `400 Bad Request`.

### Programa inexistente

Usa un UUID válido pero que no exista:

```http
POST http://localhost:8080/api/academic/programs/00000000-0000-0000-0000-000000000000/chips
```

Respuesta esperada:

```json
{
  "message": "Programa no encontrado"
}
```

### Programa inactivo

Desactiva un programa y luego intenta crear una ficha dentro de él.

Respuesta esperada:

```json
{
  "message": "No se puede crear una ficha en un programa inactivo"
}
```

### Ficha duplicada

Crea dos fichas con el mismo `chipCode`.

Respuesta esperada:

```json
{
  "message": "Ya existe una ficha con ese código"
}
```

## 7. Pruebas de seguridad

### Sin token

Llama a cualquier endpoint académico sin `Authorization`:

```http
GET http://localhost:8080/api/academic/programs
```

Debe ser rechazado por Spring Security.

### Con usuario sin rol Coordinador

Usa un JWT válido de Instructor o Aprendiz.

Debe ser rechazado porque las rutas académicas solo permiten `COORDINATOR`.

### Con Coordinador

Usa un JWT válido con autoridad de rol `ROLE_COORDINATOR`.

Las operaciones deben poder ejecutarse.

## 8. Verificación del historial

Después de crear, actualizar, desactivar, reactivar o eliminar un programa/ficha, revisa la tabla:

```sql
SELECT entity_name,
       entity_id,
       field_name,
       old_value,
       new_value,
       action,
       created_at
FROM academic.change_history
ORDER BY created_at DESC;
```

Debes encontrar acciones como:

```text
CREATE
UPDATE
DEACTIVATE
REACTIVATE
DELETE
```

## 9. Verificación directa de datos

### Programas

```sql
SELECT id_program, program_name, program_code, state, deactivation_reason
FROM academic.program
ORDER BY created_at DESC;
```

### Fichas

```sql
SELECT id_chip, id_program, chip_code, state, deactivation_reason
FROM academic.chip
ORDER BY created_at DESC;
```

### Relaciones futuras ya modeladas

```sql
SELECT * FROM academic.instructor_program;
SELECT * FROM security.user_chip;
```

## 10. Resultado esperado de la prueba completa

La prueba es correcta si se confirma lo siguiente:

- Se puede crear un programa activo.
- No se pueden repetir nombre ni código.
- Se puede editar nombre y código sin cambiar el estado.
- Se puede crear una ficha dentro de un programa activo.
- Una ficha siempre queda asociada a un único programa.
- El código de ficha exige 7 números.
- La primera eliminación cambia a `INACTIVE`.
- La reactivación devuelve a `ACTIVE`.
- Un programa con fichas no se puede eliminar físicamente.
- Una ficha con aprendices no se puede eliminar físicamente.
- Cada operación relevante queda registrada en `academic.change_history`.
- Solo un Coordinador puede acceder a las rutas académicas.
