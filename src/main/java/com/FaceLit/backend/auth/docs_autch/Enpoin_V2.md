# Módulo 1: Seguridad y accesos

Base URL local: `http://localhost:8080`

Este módulo usa el esquema V4 de la base de datos. El acceso se identifica
con `security.user_app.number_document`; el correo solo se usa para enviar
la recuperación de contraseña.

## Flujo de arranque: Coordinador semilla

Ejecuta los pasos siguientes en orden. El primer Coordinador se crea durante
el despliegue de la base de datos mediante Liquibase; no existe un endpoint
especial para crearlo.

El seed completa estas tres tablas:

1. `security.user_app`: identidad, documento y estado `ACTIVE`.
2. `security.credential`: correo, hash BCrypt y estado `ACTIVE`.
3. `roleandpermission.user_role`: rol `COORDINADOR`.

El script es `src/main/resources/db/changelog/02_dml/seed_coordinator.sql`.

> Importante: después de ejecutar un changeset en un ambiente compartido no
> se debe editar su contenido sin controlar el checksum. Este changeset ya
> acepta el checksum registrado antes de agregar las validaciones del seed,
> por lo que no es necesario borrar filas ni modificar manualmente
> `DATABASECHANGELOG`.

### Paso 1. Confirmar prerrequisitos

Antes de ejecutar el backend, deben existir:

- Los esquemas `security` y `roleandpermission`.
- Las tablas V4, incluyendo sus claves únicas.
- El rol `COORDINADOR` en `roleandpermission.role`.
- La extensión/función `uuid_generate_v4()`.
- Las variables de conexión `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

Comprueba el rol con:

```sql
SELECT id_role, name_rol
FROM roleandpermission.role
WHERE name_rol = 'COORDINADOR';
```

Debe devolver una fila. Si no devuelve filas, primero ejecuta el changeset
que inserta los roles base; el seed no puede asignar un rol inexistente.

### Paso 2. Elegir los datos del ambiente

Define un documento y correo exclusivos para ese ambiente. No uses un
documento genérico como `0000000000` en producción si la institución tiene
un documento real para la cuenta administrativa.

Los valores que necesita el changeset son:

| Variable | Uso | ¿Se puede versionar? |
|---|---|---|
| `SEED_COORDINATOR_FIRST_NAME` | Nombre | No es secreto, pero se configura por ambiente |
| `SEED_COORDINATOR_LAST_NAME` | Apellido | No es secreto, pero se configura por ambiente |
| `SEED_COORDINATOR_DOCUMENT` | Documento único de login | No es secreto, pero se configura por ambiente |
| `SEED_COORDINATOR_EMAIL` | Correo de recuperación | No es secreto, pero se configura por ambiente |
| `SEED_COORDINATOR_PASSWORD_HASH` | Hash BCrypt de la contraseña | Nunca en texto plano ni en Git |

### Paso 3. Generar el hash BCrypt

La contraseña debe cumplir la misma política del sistema: entre 8 y 15
caracteres, al menos una mayúscula, un número y un símbolo. Genera el hash
en una máquina segura, sin pegar la contraseña en un archivo del proyecto,
terminal compartida, issue o commit.

Genera un hash BCrypt con costo 10 usando una herramienta segura aprobada
por el equipo. El resultado debe verse parecido a:

```text
$2a$10$.............................................................
```

Guarda solamente el hash como secreto del ambiente. La contraseña original
debe entregarse por un canal seguro al Coordinador y cambiarse después del
primer acceso usando `POST /api/auth/change-password`.

No reemplaces el hash por la contraseña original. El valor que se configura
en `SEED_COORDINATOR_PASSWORD_HASH` debe comenzar con `$2a$10$`, `$2b$10$`
o el prefijo BCrypt equivalente generado por la herramienta.

### Paso 4. Configurar las variables antes de iniciar Spring

En PowerShell, durante el despliegue local:

```powershell
$env:SEED_COORDINATOR_FIRST_NAME = "Nombre"
$env:SEED_COORDINATOR_LAST_NAME = "Apellido"
$env:SEED_COORDINATOR_DOCUMENT = "1234567890"
$env:SEED_COORDINATOR_EMAIL = "coordinador@facelit.com"
$env:SEED_COORDINATOR_PASSWORD_HASH = '$2a$10$HASH_BCRYPT_GENERADO'
```

Ejemplo completo con datos ficticios:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/facelit"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "CONTRASEÑA_DE_LA_BD"
$env:JWT_SECRET = "CLAVE_JWT_DE_AL_MENOS_32_CARACTERES"
$env:MAIL_USERNAME = "correo@facelit.com"
$env:MAIL_PASSWORD = "CLAVE_SMTP"

$env:SEED_COORDINATOR_FIRST_NAME = "Coordinador"
$env:SEED_COORDINATOR_LAST_NAME = "Inicial"
$env:SEED_COORDINATOR_DOCUMENT = "1234567890"
$env:SEED_COORDINATOR_EMAIL = "coordinador@facelit.com"
$env:SEED_COORDINATOR_PASSWORD_HASH = '$2a$10$HASH_BCRYPT_GENERADO'
```

No agregues estos valores al repositorio ni los escribas en `application.yaml`.
En CI/CD deben configurarse como variables protegidas o secretos del ambiente.
Si ejecutas la aplicación con el botón de VS Code, configura esas mismas
variables en el entorno de la configuración de ejecución; definirlas en una
terminal distinta no siempre se transfiere a ese proceso.

El bloque `spring.liquibase.parameters` de `application.yaml` conecta estas
variables con los placeholders del changeset. Si alguna falta, el seed se
detiene con un mensaje explícito y no intenta insertar un placeholder literal.

### Paso 5. Ejecutar el despliegue

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

También puedes compilar y ejecutar así:

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

Liquibase ejecutará `db.changelog-master.yaml`, aplicará el changeset
`facelit:02-seed-coordinator` y registrará su ejecución. Si el arranque falla
con "Faltan variables SEED_COORDINATOR", detén el proceso, configura las
variables en la misma terminal y vuelve a ejecutar el comando.

Si aparece `was ... but is now ...`, significa que la base tiene un checksum
anterior. No ejecutes `clearCheckSums` sin conexión a la base. El comando
Maven necesita recibir explícitamente URL, usuario y contraseña, por ejemplo:

```powershell
.\mvnw.cmd liquibase:clearCheckSums `
  "-Dliquibase.url=$env:DB_URL" `
  "-Dliquibase.username=$env:DB_USERNAME" `
  "-Dliquibase.password=$env:DB_PASSWORD" `
  "-Dliquibase.changeLogFile=src/main/resources/db/changelog/db.changelog-master.yaml"
```

Para este seed no necesitas ejecutar ese comando: reinicia Spring con el
archivo actualizado y deja que Liquibase valide el `validCheckSum`.

### Paso 6. Verificar las tres relaciones

Ejecuta estas consultas en la misma base de datos:

```sql
SELECT u.id_user_app, u.first_name, u.last_name,
       u.number_document, u.account_status,
       c.email, c.credential_status, c.failed_attempts,
       c.locked_until,
       r.name_rol
FROM security.user_app u
JOIN security.credential c ON c.id_user_app = u.id_user_app
JOIN roleandpermission.user_role ur ON ur.id_user_app = u.id_user_app
JOIN roleandpermission.role r ON r.id_role = ur.id_role
WHERE u.number_document = '1234567890';
```

El resultado debe tener una sola fila con `ACTIVE`, correo activo y
`COORDINADOR`. Comprueba además que Liquibase registró el changeset:

```sql
SELECT id, author, filename, dateexecuted
FROM databasechangelog
WHERE id = '02-seed-coordinator'
  AND author = 'facelit';
```

### Paso 7. Probar el primer acceso

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "numberDocument": "1234567890",
  "password": "CONTRASEÑA_ENTREGADA_DE_FORMA_SEGURA"
}
```

La respuesta debe incluir `role: "COORDINADOR"` y los permisos sembrados
para ese rol. Después del login, cambia la contraseña desde el endpoint
autenticado y desactiva la cuenta semilla cuando ya exista una cuenta real
de Coordinador, si la política operativa de la institución lo exige.

### Paso 8. Crear las demás cuentas

Usa el token recibido en el login:

```http
Authorization: Bearer <TOKEN_DEL_COORDINADOR>
```

Desde la funcionalidad protegida de Gestión de Usuarios, el Coordinador
crea usuarios con sus datos, credencial y rol. El flujo esperado es:

1. El Coordinador inicia sesión.
2. El frontend conserva el JWT.
3. El frontend envía el JWT en cada request protegido.
4. El backend valida el rol y permisos del JWT.
5. El Coordinador crea aprendices, instructores u otros Coordinadores.
6. Cada usuario nuevo inicia sesión con su documento y contraseña.

El Coordinador semilla no usa una ruta especial después del primer despliegue:
es una cuenta normal con el rol y permisos de `COORDINADOR`.

### Paso 9. Reintentar el despliegue

Puedes reiniciar el backend o desplegar nuevamente. Liquibase consultará
`DATABASECHANGELOG` y no repetirá el changeset. Si se ejecuta el SQL en una
base que no tiene el changeset registrado pero ya contiene el documento, las
cláusulas de idempotencia conservarán las filas existentes.

### Reglas de seguridad

- Nunca pongas la contraseña plana en el SQL, `.env`, Git o documentación.
- No uses el hash de desarrollo en producción.
- No reutilices la contraseña entre ambientes.
- No crees un endpoint `/api/setup/first-coordinator`.
- El seed no otorga permisos especiales: usa el mismo rol y permisos que
  cualquier otro Coordinador.

## 1. Login

### Request

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "numberDocument": "1234567890",
  "password": "Coordinador@123"
}
```

### Respuesta `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "COORDINADOR",
  "permissions": ["VIEW_OWN_PROFILE", "MANAGE_USERS"],
  "userId": "uuid-del-usuario"
}
```

El JWT contiene `userId`, `email`, `role` y `permissions`. Los siguientes
requests deben enviar `Authorization: Bearer <token>`.

### Reglas

- La cuenta debe estar en estado `ACTIVE`.
- Después de tres contraseñas incorrectas se bloquea durante 15 minutos.
- El bloqueo se persiste en `security.credential.locked_until`.
- Un login correcto reinicia `failed_attempts` y elimina el bloqueo vencido.
- El filtro valida el JWT y sus autoridades para autorizar los requests.

## 2. Solicitar recuperación

```http
POST /api/auth/request-recovery
Content-Type: application/json
```

```json
{
  "email": "coordinador@facelit.com"
}
```

Se genera un código numérico de seis dígitos, válido durante cinco minutos,
y se envía al correo de la credencial. El código anterior activo se invalida.

Respuesta `200 OK`:

```json
{
  "message": "Se envió un código de recuperación a tu correo electrónico"
}
```

## 3. Restablecer contraseña

```http
POST /api/auth/reset-password
Content-Type: application/json
```

```json
{
  "token": "483921",
  "newPassword": "NuevaClave@123",
  "confirmPassword": "NuevaClave@123"
}
```

El código es de un solo uso. La contraseña debe tener entre 8 y 15
caracteres, una mayúscula, un número y un símbolo.

Respuesta `200 OK`:

```json
{
  "message": "Contraseña restablecida correctamente"
}
```

## 4. Cambio voluntario autenticado

```http
POST /api/auth/change-password
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "newPassword": "OtraClave@123",
  "confirmPassword": "OtraClave@123"
}
```

La contraseña anterior no se devuelve ni se expone. La nueva se almacena
como hash BCrypt y se limpian los intentos fallidos y el bloqueo temporal.

## 5. Entidades del módulo

| Entidad | Tabla | Uso |
|---|---|---|
| `User` | `security.user_app` | Identidad, documento y estado de cuenta |
| `Credential` | `security.credential` | Correo, hash, intentos y bloqueo |
| `PasswordRecovery` | `security.password_recovery` | Código, expiración y uso |
| `UserSession` | `security.user_session` | Inicio y cierre de sesiones |
| `Role` | `roleandpermission.role` | Rol del usuario |
| `Permission` | `roleandpermission.permission` | Permiso de negocio |
| `UserRole` | `roleandpermission.user_role` | Asignación de rol |
| `RolePermission` | `roleandpermission.role_permission` | Permisos por rol |
| `TermsAcceptance` | `legal.terms_acceptance` | Aceptación legal, cuando aplique |

`User` no contiene `documentType`, `birthDate` ni `emailVerified`.
`Credential` contiene `lockedUntil`. `PasswordRecovery` solo usa las
columnas V4: `token`, `expiration_date` y `used`.

## 6. Errores comunes

Todas las excepciones de negocio responden con:

```json
{
  "message": "Descripción del error"
}
```

- `400`: código vencido o usado, contraseñas diferentes o política inválida.
- `401`: documento/contraseña incorrectos, cuenta inactiva o bloqueada.
- `400`: cuenta inexistente o correo no registrado en recuperación.

## 7. Archivos principales

- Modelos: `auth/model/security` y `auth/model/roleandpermission`.
- Requests/responses: `auth/dto`.
- Persistencia: `auth/repository`.
- Reglas: `auth/service` y `auth/service/serviceImpl`.
- Rutas: `auth/controller`.
- JWT y BCrypt: `config/SecurityConfig.java` y `config/JwtFilter.java`.
- Errores: `shared/exception/GlobalExceptionHandler.java`.
