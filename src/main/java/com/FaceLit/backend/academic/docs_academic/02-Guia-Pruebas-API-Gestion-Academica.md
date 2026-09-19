# Guía de pruebas API - Gestión Académica

## 1. Preparación

### Servidor

El backend corre normalmente en:

```text
http://localhost:8080
```

### Autenticación

Las rutas académicas requieren un JWT de un usuario con rol `ROLE_COORDINATOR`.

Ejemplo de cabecera:

```http
Authorization: Bearer TU_JWT_DE_COORDINATOR
Content-Type: application/json
```

## 2. Flujo recomendado de prueba

Se recomienda ejecutar la prueba en este orden:

1. Crear un programa.
2. Guardar `idProgram`.
3. Crear una ficha dentro de ese programa.
4. Guardar `idChip`.
5. Crear un instructor `TRANSVERSAL` y luego un instructor `ESPECIFICO`.
6. Asignar un aprendiz a una ficha.
7. Consultar la ficha activa del usuario.
8. Ver transfer-targets.
9. Realizar traslado de ficha.
10. Descargar y probar la plantilla CSV.
11. Subir el CSV institucional.
12. Revisar el historial desde la API o directamente en la base de datos.

## 3. Programas

### 3.1 Crear programa

```http
POST http://localhost:8080/api/academic/programs
```

Body:

```json
{
  "programName": "Análisis y Desarrollo de Software",
  "programCode": "ADSO"
}
```

Respuesta esperada: `200 OK`

```json
{
  "idProgram": "UUID_GENERADO",
  "programName": "Análisis y Desarrollo de Software",
  "programCode": "ADSO",
  "state": "ACTIVE",
  "deactivationReason": null
}
```

### 3.2 Listar programas

```http
GET http://localhost:8080/api/academic/programs
```

### 3.3 Consultar programa por ID

```http
GET http://localhost:8080/api/academic/programs/{idProgram}
```

### 3.4 Editar programa

```http
PUT http://localhost:8080/api/academic/programs/{idProgram}
```

Body:

```json
{
  "programName": "Análisis y Desarrollo de Software - Actualizado",
  "programCode": "ADSO2"
}
```

### 3.5 Reactivar programa

```http
PATCH http://localhost:8080/api/academic/programs/{idProgram}/reactivate
```

### 3.6 Desactivar programa

```http
DELETE http://localhost:8080/api/academic/programs/{idProgram}?reason=Cierre%20temporal
```

Primera llamada esperada:

- `200 OK`
- estado pasa a `INACTIVE`
- `deactivationReason` se guarda
- se registra cambio en `change_history`

### 3.7 Eliminar físicamente un programa

```http
DELETE http://localhost:8080/api/academic/programs/{idProgram}
```

Si tiene fichas o instructores asociados, responde con `400 Bad Request`:

```json
{
  "message": "No se puede eliminar el programa porque tiene fichas o instructores asociados"
}
```

## 4. Fichas

### 4.1 Crear ficha

```http
POST http://localhost:8080/api/academic/programs/{idProgram}/chips
```

Body:

```json
{
  "idProgram": "{idProgram}",
  "chipCode": "2825551"
}
```

Respuesta esperada: `200 OK`

```json
{
  "idChip": "UUID_GENERADO",
  "idProgram": "{idProgram}",
  "chipCode": "2825551",
  "state": "ACTIVE",
  "deactivationReason": null
}
```

### 4.2 Listar fichas por programa

```http
GET http://localhost:8080/api/academic/programs/{idProgram}/chips
```

### 4.3 Consultar ficha por ID

```http
GET http://localhost:8080/api/academic/chips/{idChip}
```

### 4.4 Editar ficha

```http
PUT http://localhost:8080/api/academic/chips/{idChip}
```

Body:

```json
{
  "idProgram": "{idProgram}",
  "chipCode": "2825552"
}
```

### 4.5 Desactivar ficha

```http
DELETE http://localhost:8080/api/academic/chips/{idChip}?reason=Ficha%20cerrada
```

### 4.6 Reactivar ficha

```http
PATCH http://localhost:8080/api/academic/chips/{idChip}/reactivate
```

### 4.7 Eliminar físicamente ficha

```http
DELETE http://localhost:8080/api/academic/chips/{idChip}
```

Si tiene aprendices asociados, responde con `400 Bad Request`:

```json
{
  "message": "No se puede eliminar la ficha porque tiene aprendices asociados"
}
```

## 5. Instructores

### 5.1 Crear instructor transversal

```http
POST http://localhost:8080/api/academic/instructors
```

Body:

```json
{
  "idUser": "UUID_USUARIO",
  "instructorType": "TRANSVERSAL",
  "programIds": []
}
```

Respuesta esperada: `200 OK`.

### 5.2 Crear instructor específico

```http
POST http://localhost:8080/api/academic/instructors
```

Body:

```json
{
  "idUser": "UUID_USUARIO",
  "instructorType": "ESPECIFICO",
  "programIds": ["UUID_PROGRAMA_1", "UUID_PROGRAMA_2"]
}
```

Si `programIds` viene vacío para `ESPECIFICO`, responde:

```json
{
  "message": "Un instructor específico debe indicar el programa al que pertenece."
}
```

### 5.3 Listar instructores

```http
GET http://localhost:8080/api/academic/instructors
```

### 5.4 Consultar instructor por ID

```http
GET http://localhost:8080/api/academic/instructors/{idInstructor}
```

### 5.5 Consultar instructor por usuario

```http
GET http://localhost:8080/api/academic/instructors/user/{idUser}
```

### 5.6 Editar instructor

```http
PUT http://localhost:8080/api/academic/instructors/{idInstructor}
```

Body:

```json
{
  "instructorType": "ESPECIFICO",
  "programIds": ["UUID_PROGRAMA_1"]
}
```

Esto reemplaza la lista completa de programas; no acumula.

### 5.7 Eliminar instructor

```http
DELETE http://localhost:8080/api/academic/instructors/{idInstructor}
```

Si el usuario ya existe como instructor:

```json
{
  "message": "Este usuario ya está registrado como instructor."
}
```

## 6. Asignación inicial de ficha a aprendiz

### 6.1 Asignar aprendiz a ficha

```http
POST http://localhost:8080/api/academic/chips/{idChip}/apprentices
```

Body:

```json
{
  "idUser": "UUID_USUARIO"
}
```

Si ya tiene una ficha activa, responde:

```json
{
  "message": "Este aprendiz ya tiene una ficha activa. Usa el traslado para cambiarlo de ficha."
}
```

### 6.2 Ver aprendices activos de una ficha

```http
GET http://localhost:8080/api/academic/chips/{idChip}/apprentices
```

## 7. Traslado de ficha

### 7.1 Ver fichas destino disponibles

```http
GET http://localhost:8080/api/academic/users/{idUser}/chip/transfer-targets
```

Respuesta esperada: lista de fichas activas distintas a la actual.

### 7.2 Trasladar a otra ficha

```http
POST http://localhost:8080/api/academic/users/{idUser}/chip/transfer
```

Body:

```json
{
  "idNewChip": "UUID_FICHA_DESTINO"
}
```

Casos de error:

```json
{
  "message": "El aprendiz no tiene una ficha activa para trasladar."
}
```

```json
{
  "message": "La ficha destino no existe."
}
```

```json
{
  "message": "La ficha destino no está activa."
}
```

```json
{
  "message": "La ficha destino debe ser diferente a la actual."
}
```

```json
{
  "message": "Debes seleccionar una ficha destino."
}
```

## 8. Consultas académicas disponibles

### 8.1 Buscar instructores por documento / nombre / tipo

```http
GET http://localhost:8080/api/academic/instructors/search?document=1029&name=ana&type=ESPECIFICO
```

### 8.2 Ver ficha activa actual de un aprendiz

```http
GET http://localhost:8080/api/academic/users/{idUser}/chip
```

### 8.3 Ver historial de fichas de un aprendiz

```http
GET http://localhost:8080/api/academic/users/{idUser}/chip/history
```

### 8.4 Buscar programas por nombre

```http
GET http://localhost:8080/api/academic/programs/search?name=ADSO
```

Devuelve `200 OK` y una lista vacía si no hay resultados.

### 8.5 Buscar programa por código exacto

```http
GET http://localhost:8080/api/academic/programs/code/ADSO
```

### 8.6 Buscar fichas por código

```http
GET http://localhost:8080/api/academic/chips/search?code=2825
```

La búsqueda de fichas por código es parcial e insensible a mayúsculas.

### 8.7 Consultar historial por entidad

```http
GET http://localhost:8080/api/academic/change-history?entityName=instructor&entityId={id}
```

La respuesta llega ordenada de la modificación más reciente a la más antigua.

## 9. Errores y excepciones esperadas

### 9.1 Mensajes de error académicos

Todos los errores del módulo académico están centralizados en `AcademicException` y se retornan como JSON:

```json
{
  "message": "Texto del error"
}
```

Ejemplos:

```json
{
  "message": "Usuario no encontrado."
}
```

```json
{
  "message": "Este usuario ya está registrado como instructor."
}
```

```json
{
  "message": "El programa indicado no existe."
}
```

```json
{
  "message": "Instructor no encontrado."
}
```

```json
{
  "message": "No es posible eliminar. Este instructor tiene registros de asistencia asociados."
}
```

### 9.2 Status HTTP reales

El manejador global usa el estado HTTP correcto según el tipo de error:

- `400 Bad Request`: validación de negocio o datos inválidos
- `404 Not Found`: recurso no encontrado
- `409 Conflict`: conflicto de negocio (usuario ya instructor o aprendizaje ya tiene ficha activa)

## 10. Verificación del historial de cambios

Consulta:

```sql
SELECT entity_name, entity_id, field_name, old_value, new_value, action, created_at
FROM academic.change_history
ORDER BY created_at DESC;
```

Debe observarse historial tipo:

- `instructor` / `CREATE` / `instructor_type`
- `instructor` / `UPDATE` / `instructor_program`
- `user_chip` / `CREATE` / `chip`
- `user_chip` / `UPDATE` / `chip`

## 11. Verificación directa de datos

### Instructores

```sql
SELECT * FROM academic.instructor;
SELECT * FROM academic.instructor_program;
```

### Aprendices y fichas

```sql
SELECT * FROM security.user_chip;
SELECT * FROM academic.chip;
```

## 12. Resultado esperado de la prueba completa

La prueba es correcta si se confirma:

- instructor `TRANSVERSAL` se crea sin programas
- instructor `ESPECIFICO` exige programas
- `eligible?idProgram=` devuelve transversales y específicos del programa
- un aprendiz no tiene dos fichas activas
- traslado deja la ficha anterior `INACTIVE` y la nueva `ACTIVE`
- el historial registra los cambios correspondientes
- la búsqueda de instructores acepta filtros combinables
- las consultas sin resultados de instructores devuelven `200` con lista vacía

## 13. Carga institucional CSV - Día 3

### 13.1 Descargar plantilla

```http
GET http://localhost:8080/api/academic/csv/template
```

La respuesta es un archivo `text/csv` descargable llamado
`plantilla-carga-academica.csv`.

### 13.2 Formato del archivo

El encabezado debe ser:

```csv
tipo,documento,nombre,apellido,correo,programa_codigo,ficha_codigo,instructor_tipo
```

Ejemplo:

```csv
programa,,,,,ADSO,,
ficha,,,,,ADSO,2825551,
aprendiz,1002345678,Juan,Perez,juan.perez@correo.com,,2825551,
instructor,1029384756,Laura,Gomez,laura.gomez@correo.com,ADSO,,especifico
instructor,1050607080,Carlos,Ruiz,carlos.ruiz@correo.com,,,transversal
```

Aunque las filas estén mezcladas, el backend las procesa por fases: programa,
ficha, instructor y aprendiz. La respuesta mantiene el número real de línea.

### 13.3 Subir CSV

La petición es `multipart/form-data`; el nombre de la parte debe ser `file`:

```http
POST http://localhost:8080/api/academic/csv/upload
```

Respuesta `200 OK`:

```json
{
  "creados": [],
  "actualizados": [],
  "inconsistenciasBloqueadas": [],
  "erroresDeReferencia": [],
  "trasladosPendientes": [],
  "contrasenasGeneradas": []
}
```

Las seis listas siempre aparecen, aunque estén vacías. Las contraseñas
temporales solo se muestran una vez en esta respuesta; en la base de datos se
guarda únicamente su hash.

### 13.4 Reglas principales de carga

- extensión `.csv`
- UTF-8 válido
- máximo 5 MB
- máximo 5000 filas de datos
- `programa_codigo`: 2 a 15 caracteres alfanuméricos
- `ficha_codigo`: exactamente 7 dígitos
- documentos: exactamente 10 dígitos
- `instructor_tipo`: `especifico` o `transversal`
- un instructor existente conserva sus programas y puede acumular nuevos
- cambiar el tipo de un instructor existente queda bloqueado
- cambiar el programa de una ficha existente queda bloqueado

Los errores estructurales del archivo completo responden `400` y no procesan
ninguna fila. Los errores de una fila aparecen en el resumen y permiten
continuar con las demás filas.

Ejemplo de archivo demasiado grande:

```json
{
  "message": "El archivo supera el máximo de 5000 filas permitidas por carga. Divide la información en varios archivos."
}
```

### 13.5 Consultar traslados pendientes

```http
GET http://localhost:8080/api/academic/csv/pending-transfers
```

Devuelve solo propuestas con estado `PENDING`. La carga CSV no ejecuta el
traslado automáticamente.

### 13.6 Aceptar un traslado pendiente

```http
POST http://localhost:8080/api/academic/csv/pending-transfers/{idPendingTransfer}/accept
```

El backend reutiliza el servicio de traslado normal del Día 2, marca la
propuesta como `ACCEPTED` y registra `CSV_CONFIRM` en `change_history`.

### 13.7 Cancelar un traslado pendiente

```http
POST http://localhost:8080/api/academic/csv/pending-transfers/{idPendingTransfer}/cancel
```

Respuesta esperada: `204 No Content`. La ficha del aprendiz no cambia y se
registra `CSV_CANCEL`.

Errores de resolución:

```json
{
  "message": "Traslado pendiente no encontrado."
}
```

```json
{
  "message": "Este traslado pendiente ya fue resuelto anteriormente."
}
![alt text](image.png)
```

### 13.8 Verificación SQL del Día 3

```sql
SELECT *
FROM academic.csv_pending_transfer
ORDER BY created_at DESC;

SELECT entity_name, entity_id, field_name, old_value, new_value, action, created_at
FROM academic.change_history
WHERE action IN ('CSV_CONFIRM', 'CSV_CANCEL')
ORDER BY created_at DESC;
```

La tabla `academic.csv_pending_transfer` debe existir previamente en el
esquema `academic` de la base de datos. Su creación y cambios se gestionan
desde el repositorio de base de datos, no desde este backend.
