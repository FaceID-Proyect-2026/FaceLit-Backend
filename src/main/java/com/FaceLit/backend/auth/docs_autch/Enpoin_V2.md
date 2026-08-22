# Endpoints y JSON de prueba - FaceLit Backend

### BASE URL

```
http://localhost:8080
```

> ⚠️ **Formato de errores (actualizado):** todos los errores del backend ahora devuelven la clave `"message"`, no `"error"`. Ejemplo: `{ "message": "El email ya esta registrado" }`.

---

## 1. Módulo: Catálogos

> Este módulo va de primero porque sus datos (por ejemplo, los tipos de documento) se necesitan antes de poder registrar un usuario en el Módulo 2.

### 1.1 Listar Tipos de Documento

**Endpoint**

```http
GET /api/catalogos/document-types
```

**Respuesta esperada**

```json
[
  { "idDocumentType": "bf8b39c0-15ae-4303-8323-8ca6bf476318", "name": "CITIZENSHIP CARD", "abbreviation": "CC" },
  { "idDocumentType": "b0793264-321a-462b-a7e8-a2c3a3e1f2...", "name": "FOREIGNER IDENTITY CARD", "abbreviation": "CE" },
  { "idDocumentType": "52447d51-b462-4a75-8974-fc9c971acdfc", "name": "IDENTITY CARD", "abbreviation": "TI" },
  { "idDocumentType": "36a21a19-0c0d-43df-9ef0-58284b1d4aac", "name": "PASSPORT", "abbreviation": "PAS" }
]
```

---

## 2. Módulo: Autenticación y Registro de Usuario

### 2.1 Registro de Usuario

> ⚠️ **ACTUALIZADO:** el request ahora incluye el campo `accepted` (aceptación de términos y condiciones), que antes se mandaba por separado al endpoint `/api/auth/aceptar-terminos`. Ahora todo se guarda en una sola transacción.

**Endpoint**

```http
POST /api/auth/register
```

**JSON — Usuario mayor de edad**

```json
{
  "idDocumentType": "UUID_DEL_TIPO_CC",
  "firstName": "Maria",
  "lastName": "Oyola",
  "documentNumber": "1234567890",
  "birthDate": "2000-05-10",
  "email": "maria@gmail.com",
  "password": "Maria@123",
  "accepted": true
}
```

**JSON — Usuario menor de edad**

```json
{
  "idDocumentType": "UUID_DEL_TIPO_TI",
  "firstName": "Juan",
  "lastName": "Perez",
  "documentNumber": "1099999999",
  "birthDate": "2012-08-15",
  "email": "juan@gmail.com",
  "password": "Juan@123",
  "accepted": true
}
```

**Respuesta esperada**

HTTP STATUS
```
202 ACCEPTED
```

Response
```json
{
  "message": "PENDING_EMAIL_VERIFICATION",
  "id_user": "UUID_DEL_USUARIO"
}
```

**Validaciones que debes probar**

Documento repetido — debe mostrar:
```
El numero de documento ya esta registrado
```

Correo repetido — debe mostrar:
```
El email ya esta registrado
```

TI siendo mayor de edad — debe mostrar:
```
La Tarjeta de Identidad es solo para menores de edad
```

CC siendo menor de edad — debe mostrar:
```
La Cedula de cuidadania solo es para mayores de edad
```

No aceptó los términos (`"accepted": false`) — debe mostrar:
```
No puede continuar sin confirmar lectura o aceptar responsabilidad
```

---

### 2.2 Verificar Email

**Endpoint**

```http
POST /api/auth/verify-email
```

**JSON**

```json
{
  "id_user": "UUID_DEL_USUARIO",
  "code": "123456"
}
```

**Respuesta esperada**

Mayor de edad
```json
{
  "message": "ACCOUNT_ACTIVE"
}
```

Menor de edad
```json
{
  "message": "PENDING_GUARDIAN_CONSENT"
}
```

**Validaciones**

Código incorrecto:
```
Código incorrecto
```

Código expirado:
```
El código ha expirado. Solicita uno nuevo.
```

---

### 2.3 Reenviar Código

**Endpoint**

```http
POST /api/auth/resend-code?id_user=UUID_DEL_USUARIO
```

**Respuesta**

```json
{
  "message": "Código reenviado a tu correo electrónico"
}
```

> ⚠️ **ACTUALIZADO:** ahora tiene un cooldown de 5 minutos entre reenvíos. Si se solicita antes de tiempo, responde con un mensaje indicando cuántos segundos faltan.

---

### 2.4 Solicitar Consentimiento del Acudiente

**Endpoint**

```http
POST /api/consent/request
```

**JSON**

```json
{
  "id_user": "UUID_DEL_USUARIO_MENOR",
  "fullName": "Carlos Perez",
  "identityDocument": "111111111",
  "emailGuardian": "acudiente@gmail.com"
}
```

**Validaciones**

Correo del acudiente igual al del menor:
```
El correo del acudiente debe ser diferente al del usuario
```

**Respuesta esperada**

```json
{
  "message": "WAITING_GUARDIAN_CONFIRMATION",
  "status": "PENDING",
  "idConsent": "UUID_DEL_CONSENTIMIENTO"
}
```

#### 2.4.1 Reenviar Solicitud de Consentimiento

Se utiliza cuando el código anterior expiró y el menor necesita que el acudiente reciba uno nuevo.

**Endpoint**

```http
POST /api/consent/resend
```

**JSON**

```json
{
  "id_user": "UUID_DEL_USUARIO_MENOR"
}
```

**Respuesta esperada**

```json
{
  "message": "WAITING_GUARDIAN_CONFIRMATION",
  "status": "PENDING",
  "idConsent": "UUID_DEL_CONSENTIMIENTO"
}
```

> ⚠️ **ACTUALIZADO:** también tiene cooldown de 5 minutos entre reenvíos, igual que 2.3.

---

### 2.5 Aceptar Consentimiento

> ⚠️ **CAMBIÓ COMPLETAMENTE:** ya no se usa un `token` UUID largo (por link de correo). Ahora el acudiente recibe un **código de 6 dígitos** por correo y lo ingresa junto con el `id_user` del menor, igual que la verificación de email.

**Endpoint**

```http
POST /api/consent/confirm
```

**JSON**

```json
{
  "id_user": "UUID_DEL_USUARIO_MENOR",
  "code": "123456"
}
```

**Respuesta esperada**

```json
{
  "message": "Autorización aceptada. El menor ya puede acceder al sistema.",
  "status": "ACCEPTED"
}
```

**Validaciones**

Código incorrecto:
```json
{ "message": "Código incorrecto" }
```

Código expirado:
```json
{
  "message": "El enlace ha expirado. El menor debe solicitar uno nuevo.",
  "status": "EXPIRED"
}
```

---

### 2.6 Rechazar Consentimiento

> ⚠️ **CAMBIÓ COMPLETAMENTE:** mismo cambio que 2.5 — código de 6 dígitos + `id_user`, ya no `token`.

**Endpoint**

```http
POST /api/consent/refuse
```

**JSON**

```json
{
  "id_user": "UUID_DEL_USUARIO_MENOR",
  "code": "123456"
}
```

**Respuesta esperada**

```json
{
  "message": "Autorización rechazada. El registro del menor no fue completado.",
  "status": "REFUSED"
}
```

---

### 2.7 Aceptar Términos y Condiciones

> ⚠️ **YA NO SE USA EN EL FLUJO DE REGISTRO.** El endpoint sigue existiendo y funcionando en el backend, pero el frontend ya no lo llama por separado — la aceptación de términos ahora viaja dentro del mismo `POST /api/auth/register` (ver sección 2.1, campo `accepted`). Se deja documentado por si se necesita a futuro (ej: re-aceptar términos actualizados).

**Endpoint**

```http
POST /api/auth/aceptar-terminos
```

**JSON — Correcto**

```json
{
  "id_user": "UUID_DEL_USUARIO",
  "accepted": true
}
```

**Respuesta esperada**

```json
{
  "message": "Términos aceptados correctamente"
}
```

**Validaciones**

Usuario no acepta los términos:
```json
{
  "id_user": "UUID_DEL_USUARIO",
  "accepted": false
}
```
Respuesta:
```json
{
  "message": "No puede continuar sin confirmar lectura o aceptar responsabilidad"
}
```

Usuario acepta los términos más de una vez:
```json
{
  "id_user": "UUID_DEL_USUARIO",
  "accepted": true
}
```
Respuesta:
```json
{
  "message": "El usuario ya aceptó los términos anteriormente"
}
```

Usuario no existe:
```json
{
  "id_user": "UUID_INEXISTENTE",
  "accepted": true
}
```
Respuesta:
```json
{
  "message": "Usuario no encontrado"
}
```

---

### 2.8 Inicio de Sesión y Definición de Rol

**Endpoint**

```http
POST /api/auth/login
```

**JSON**

```json
{
  "email": "maria@gmail.com",
  "password": "Maria@123",
  "aceptoPoliticas": true
}
```

**Respuesta esperada**

HTTP STATUS
```
200 OK
```

Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "INSTRUCTOR",
  "permissions": [
    "VIEW_OWN_PROFILE",
    "VIEW_OWN_ATTENDANCE",
    "VIEW_FICHA_ATTENDANCE",
    "MANAGE_SCHEDULES"
  ],
  "userId": "UUID_DEL_USUARIO",
  "message": "Inicio de sesión exitoso"
}
```

**Validaciones que debes probar**

Políticas no aceptadas:
```json
{
  "email": "maria@gmail.com",
  "password": "Maria@123",
  "aceptoPoliticas": false
}
```
Respuesta:
```json
{
  "message": "Debe aceptar las políticas de privacidad"
}
```

Correo no registrado:
```json
{
  "email": "noexiste@gmail.com",
  "password": "Maria@123",
  "aceptoPoliticas": true
}
```
Respuesta:
```json
{
  "message": "Correo electrónico no registrado"
}
```

Contraseña incorrecta:
```json
{
  "email": "maria@gmail.com",
  "password": "Incorrecta@123",
  "aceptoPoliticas": true
}
```
Respuesta:
```json
{
  "message": "Usuario o contraseña incorrectos"
}
```

Cuenta no activa (email no verificado o pendiente de acudiente):
```json
{
  "email": "juan@gmail.com",
  "password": "Juan@123",
  "aceptoPoliticas": true
}
```
Respuesta:
```json
{
  "message": "La cuenta no está activa. Verifica tu correo o contacta al administrador"
}
```

Usuario sin rol asignado (el administrador aún no le ha asignado rol):
```json
{
  "email": "nuevo@gmail.com",
  "password": "Nuevo@123",
  "aceptoPoliticas": true
}
```
Respuesta:
```json
{
  "message": "Sin permisos asignados, contacta al administrador"
}
```

**Uso del token en requests siguientes**

Una vez obtenido el token, se debe enviar en el header de cada request protegido:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuestas por rol**

Login con ADMINISTRATOR:
```json
{
  "token": "eyJhbGci...",
  "role": "ADMINISTRATOR",
  "permissions": [
    "VIEW_OWN_PROFILE",
    "EDIT_OWN_PROFILE",
    "VIEW_ALL_USERS",
    "MANAGE_USERS",
    "ASSIGN_ROLES",
    "VIEW_ALL_ATTENDANCE",
    "VIEW_OWN_ATTENDANCE",
    "VIEW_FICHA_ATTENDANCE",
    "MANAGE_SCHEDULES"
  ],
  "userId": "UUID_DEL_USUARIO",
  "message": "Inicio de sesión exitoso"
}
```

Login con INSTRUCTOR:
```json
{
  "token": "eyJhbGci...",
  "role": "INSTRUCTOR",
  "permissions": [
    "VIEW_OWN_PROFILE",
    "EDIT_OWN_PROFILE",
    "VIEW_OWN_ATTENDANCE",
    "VIEW_FICHA_ATTENDANCE",
    "MANAGE_SCHEDULES"
  ],
  "userId": "UUID_DEL_USUARIO",
  "message": "Inicio de sesión exitoso"
}
```

Login con APPRENTICE:
```json
{
  "token": "eyJhbGci...",
  "role": "APPRENTICE",
  "permissions": [
    "VIEW_OWN_PROFILE",
    "EDIT_OWN_PROFILE",
    "VIEW_OWN_ATTENDANCE"
  ],
  "userId": "UUID_DEL_USUARIO",
  "message": "Inicio de sesión exitoso"
}
```

---

### 2.9 Consultar Estado de un Registro (NUEVO)

> Se usa cuando alguien intenta registrarse de nuevo con un documento o correo que ya existe. El frontend consulta este endpoint para saber exactamente en qué paso quedó ese registro (falta verificar email, falta consentimiento del acudiente, o falta el reconocimiento facial) y redirige automáticamente ahí, en vez de dejar al usuario sin opciones.

**Endpoint**

```http
GET /api/auth/registration-status?document=1234567890&email=maria@gmail.com
```

> Se puede mandar solo `document` o solo `email` — no hace falta enviar los dos parámetros.

**Respuesta esperada**

```json
{
  "idUser": "UUID_DEL_USUARIO",
  "emailVerified": true,
  "accountStatus": "PENDING_CONSENT",
  "isMinor": true,
  "consentStatus": "PENDING",
  "guardianEmail": "acudiente@gmail.com"
}
```

**Validaciones**

Sin coincidencias:
```json
{
  "message": "No se encontró un registro con esos datos"
}
```

---

## 3. Módulo: Restablecimiento de Contraseña (NUEVO)

### 3.1 Solicitar Recuperación de Contraseña

**Endpoint**

```http
POST /api/auth/request-recovery
```

**JSON**

```json
{
  "email": "maria@gmail.com"
}
```

**Respuesta esperada**

```json
{
  "message": "Se envió un código de recuperación a tu correo electrónico"
}
```

**Validaciones que debes probar**

Correo no registrado:
```json
{
  "email": "noexiste@gmail.com"
}
```
Respuesta:
```json
{
  "message": "Correo no registrado"
}
```

---

### 3.2 Restablecer Contraseña

**Endpoint**

```http
POST /api/auth/reset-password
```

**JSON**

```json
{
  "token": "483920",
  "newPassword": "Nueva@123",
  "confirmPassword": "Nueva@123"
}
```

**Respuesta esperada**

```json
{
  "message": "Contraseña restablecida correctamente"
}
```

**Validaciones que debes probar**

Código incorrecto o inexistente:
```json
{
  "token": "000000",
  "newPassword": "Nueva@123",
  "confirmPassword": "Nueva@123"
}
```
Respuesta:
```json
{
  "message": "Código incorrecto"
}
```

Código ya usado:
```json
{
  "token": "483920",
  "newPassword": "Otra@123",
  "confirmPassword": "Otra@123"
}
```
Respuesta:
```json
{
  "message": "Código incorrecto"
}
```

Código vencido (después de 5 minutos):
```json
{
  "token": "483920",
  "newPassword": "Nueva@123",
  "confirmPassword": "Nueva@123"
}
```
Respuesta:
```json
{
  "message": "Código vencido"
}
```

Las contraseñas no coinciden:
```json
{
  "token": "483920",
  "newPassword": "Nueva@123",
  "confirmPassword": "Diferente@123"
}
```
Respuesta:
```json
{
  "message": "Las contraseñas no coinciden"
}
```

Contraseña no cumple políticas (sin mayúscula, número o símbolo):
```json
{
  "token": "483920",
  "newPassword": "nueva123",
  "confirmPassword": "nueva123"
}
```
Respuesta:
```json
{
  "message": "La contraseña debe tener entre 8 y 15 caracteres, una mayúscula, un número y un símbolo"
}
```

---

### 3.3 Flujo Completo de Prueba

```
1. POST /api/auth/request-recovery con el email registrado
   → revisa el correo y copia el código de 6 dígitos

2. POST /api/auth/reset-password con ese código + nueva contraseña dos veces
   → contraseña actualizada

3. POST /api/auth/login con la contraseña ANTERIOR
   → debe fallar con "Usuario o contraseña incorrectos"

4. POST /api/auth/login con la contraseña NUEVA
   → debe entrar correctamente con el JWT
```

---

## 4. Flujo Completo de Prueba — Registro (actualizado)

```
1. GET /api/catalogos/document-types
   → copia el idDocumentType del tipo que vayas a usar

2. POST /api/auth/register (con "accepted": true incluido)
   → copia el id_user de la respuesta

3. POST /api/auth/verify-email con ese id_user + el código de 6 dígitos del correo
   → si es mayor de edad: "ACCOUNT_ACTIVE" → ya puede hacer login
   → si es menor de edad: "PENDING_GUARDIAN_CONSENT" → continúa al paso 4

4. (Solo menores) POST /api/consent/request con los datos del acudiente
   → copia el id_user del menor (ya lo tenías)

5. (Solo menores) POST /api/consent/confirm con id_user + código de 6 dígitos
   que llegó al correo del acudiente
   → "ACCEPTED" → el menor ya puede hacer login

6. POST /api/auth/login con el email y password del usuario
```

---

## 5. Módulo: Asignación de Roles

### BASE URL

```text
http://localhost:8080
```

> ⚠️ **Nota de consistencia:** el endpoint `GET /api/admin/users` de esta sección (5.2) devuelve el formato antiguo (`userId`, `firstName`, `lastName`, `email`, `documentNumber`, `currentRole`), correspondiente a `AdminRoleController`. El **Módulo 10** (sección 7 de este documento) tiene su propio `GET /api/admin/users` con un formato más completo (`UserDetailResponseDTO`), correspondiente a `UserManagementController`. Ambos controllers no pueden coexistir registrando la misma ruta a la vez — revisa cuál de los dos tienes activo en tu backend actual antes de probar, para no confundir las respuestas esperadas.

### 5.1 Scripts SQL de Apoyo

```sql
-- 1. Ver los usuarios que tienes
SELECT u.id_user_app, u.first_name, u.last_name, c.email, ur.id_role
FROM security.user_app u
JOIN security.credential c ON c.id_user_app = u.id_user_app
LEFT JOIN roleandpermission.user_role ur ON ur.id_user_app = u.id_user_app;

-- 2. Ver los roles disponibles
SELECT id_role, name_role FROM roleandpermission.role;

-- 3. Actualizar el rol del usuario que quieres hacer ADMINISTRATOR
-- Reemplaza los UUIDs con los que obtuviste arriba
UPDATE roleandpermission.user_role
SET id_role = 'UUID_DEL_ROL_ADMINISTRATOR',
    assignment_date = NOW(),
    assigned_at = NOW(),
    updated_at = NOW()
WHERE id_user_app = 'UUID_DEL_USUARIO';
```

---

### 5.2 Listar Todos los Usuarios

**Endpoint**

```http
GET /api/admin/users
```

**Headers**

```http
Authorization: Bearer TOKEN_DEL_ADMIN
```

**Respuesta esperada (200 OK)**

```json
[
  {
    "userId": "uuid-del-usuario",
    "firstName": "Maria",
    "lastName": "Oyola",
    "email": "maria@gmail.com",
    "documentNumber": "1234567890",
    "currentRole": "APPRENTICE"
  },
  {
    "userId": "uuid-de-otro",
    "firstName": "Juan",
    "lastName": "Perez",
    "email": "juan@gmail.com",
    "documentNumber": "0987654321",
    "currentRole": "APPRENTICE"
  }
]
```

---

### 5.3 Cambiar Rol de un Usuario

**Endpoint**

```http
PUT /api/admin/users/{UUID_DEL_USUARIO}/role
```

**Headers**

```http
Authorization: Bearer TOKEN_DEL_ADMIN
Content-Type: application/json
```

**JSON de prueba**

```json
{
  "role": "INSTRUCTOR"
}
```

**Respuesta esperada (200 OK)**

```json
{
  "userId": "uuid-del-usuario",
  "role": "INSTRUCTOR",
  "message": "Rol asignado correctamente"
}
```

---

### 5.4 Validaciones

#### 5.4.1 Solicitud sin token

**Endpoint**

```http
GET /api/admin/users
```

**Respuesta esperada**

```http
403 Forbidden
```

---

#### 5.4.2 Token de APPRENTICE o INSTRUCTOR

**Endpoint**

```http
GET /api/admin/users
```

**Headers**

```http
Authorization: Bearer TOKEN_DEL_APPRENTICE
```

o

```http
Authorization: Bearer TOKEN_DEL_INSTRUCTOR
```

**Respuesta esperada**

```http
403 Forbidden
```

---

#### 5.4.3 Usuario inexistente

**Endpoint**

```http
PUT /api/admin/users/UUID_INEXISTENTE/role
```

**Headers**

```http
Authorization: Bearer TOKEN_DEL_ADMIN
Content-Type: application/json
```

**JSON**

```json
{
  "role": "INSTRUCTOR"
}
```

**Respuesta esperada**

```json
{
  "message": "Usuario no encontrado"
}
```

---

#### 5.4.4 Rol inválido

**Endpoint**

```http
PUT /api/admin/users/{UUID_DEL_USUARIO}/role
```

**Headers**

```http
Authorization: Bearer TOKEN_DEL_ADMIN
Content-Type: application/json
```

**JSON**

```json
{
  "role": "SUPERADMIN"
}
```

**Respuesta esperada**

```http
400 Bad Request
```

---

### 5.5 Resumen de Pruebas

| Caso                            | Resultado esperado      |
| ------------------------------- | ----------------------- |
| Listar usuarios con token ADMIN | ✅ 200 OK                |
| Cambiar rol correctamente       | ✅ 200 OK                |
| Sin token                       | ❌ 403 Forbidden         |
| Token APPRENTICE                | ❌ 403 Forbidden         |
| Token INSTRUCTOR                | ❌ 403 Forbidden         |
| Usuario inexistente             | ❌ Usuario no encontrado |
| Rol no válido                   | ❌ 400 Bad Request       |

---

### 5.6 Endpoints Protegidos por Rol

| Endpoint | ADMINISTRATOR | INSTRUCTOR | APPRENTICE |
|---|---|---|---|
| `/api/admin/**` | ✅ | ❌ | ❌ |
| `/api/instructor/**` | ✅ | ✅ | ❌ |
| `/api/apprentice/**` | ✅ | ✅ | ✅ |

Si un rol intenta acceder a un endpoint que no le corresponde, el sistema devuelve:
```
HTTP 403 Forbidden
```

---

### 5.7 Anexo — Script SQL (repetido en el documento original)

> Nota: este bloque aparece nuevamente al final del documento original. Se conserva aquí tal cual, sin eliminarlo, para no perder contenido.

```sql
-- 1. Ver los usuarios que tienes
SELECT u.id_user_app, u.first_name, u.last_name, c.email, ur.id_role
FROM security.user_app u
JOIN security.credential c ON c.id_user_app = u.id_user_app
LEFT JOIN roleandpermission.user_role ur ON ur.id_user_app = u.id_user_app;

-- 2. Ver los roles disponibles
SELECT id_role, name_role FROM roleandpermission.role;

-- 3. Actualizar el rol del usuario que quieres hacer ADMINISTRATOR
-- Reemplaza los UUIDs con los que obtuviste arriba
UPDATE roleandpermission.user_role
SET id_role = 'UUID_DEL_ROL_ADMINISTRATOR',
    assignment_date = NOW(),
    assigned_at = NOW(),
    updated_at = NOW()
WHERE id_user_app = 'UUID_DEL_USUARIO';
```

---

## 6. Módulo: Configuración

### BASE URL

```
http://localhost:8080
```

### 6.1 Crear Configuración (primera vez)

**Endpoint**

```http
POST /api/profile/configuration 
```

**Headers**

```http
Authorization: Bearer TOKEN_CUALQUIER_ROL
Content-Type: application/json
```

**JSON de prueba**

```json
{
  "configurationName": "Mi configuracion",
  "description": "Configuracion principal",
  "notificationsActive": true,
  "darkMode": false,
  "language": "ES"
}
```

---

### 6.2 Actualizar Configuración

**Endpoint**

```http
PUT /api/profile/configuration 
```

**Headers**

```http
Authorization: Bearer TOKEN_CUALQUIER_ROL
```

**JSON de prueba**

```json
{
  "configurationName": "Mi configuracion",
  "description": "Cambie a modo oscuro e ingles",
  "notificationsActive": true,
  "darkMode": true,
  "language": "EN"
}
```

---

### 6.3 Consultar Configuración

**Endpoint**

```http
GET /api/profile/configuration 
```

**Headers**

```http
Authorization: Bearer TOKEN_CUALQUIER_ROL
```

---

## 7. Módulo: Gestión de Usuarios (RF-10)

### BASE URL

```text
http://localhost:8080
```

> Todos los endpoints de este módulo requieren `Authorization: Bearer {token}` de un usuario con rol `ADMINISTRATOR` o `COORDINATOR` (ambos tienen los mismos permisos sobre este módulo, incluida la eliminación).
>
> ⚠️ Ver la nota de consistencia en la sección 5 sobre el choque de ruta `GET /api/admin/users` entre `AdminRoleController` y `UserManagementController`.

### 7.1 RF-10.1 — Panel de usuarios registrados

#### 7.1.1 Listar todos los usuarios con sesión registrada

**Endpoint**

```http
GET /api/admin/users
```

**Headers**

```text
Authorization: Bearer TOKEN_ADMIN_O_COORDINATOR
```

**Respuesta esperada**

HTTP STATUS

```text
200 OK
```

Response

```json
[
  {
    "userId": "5f08e1c5-5362-4860-be05-6ebac5d7607f",
    "firstName": "Juan",
    "lastName": "Pérez",
    "documentNumber": "1035678912",
    "documentType": "CITIZENSHIP CARD",
    "birthDate": "1998-04-12",
    "email": "juan@gmail.com",
    "role": "INSTRUCTOR",
    "accountStatus": "ACTIVE",
    "registrationDate": "2026-07-01T10:15:30",
    "chipName": null,
    "chipCode": null,
    "programName": null,
    "hasSession": true
  },
  {
    "userId": "3f634480-63a4-427e-934b-5519f41a86b5",
    "firstName": "María",
    "lastName": "Gómez",
    "documentNumber": "1098765432",
    "documentType": "CITIZENSHIP CARD",
    "birthDate": "2005-02-20",
    "email": "maria@gmail.com",
    "role": "APPRENTICE",
    "accountStatus": "ACTIVE",
    "registrationDate": "2026-07-03T09:00:00",
    "chipName": "Ficha ADSO 2025",
    "chipCode": "A3F9K2M7",
    "programName": "ADSO",
    "hasSession": true
  }
]
```

**Notas de este endpoint**
- Solo aparecen usuarios que tienen al menos un registro en `user_session` (han iniciado sesión alguna vez).
- Si el usuario no es `APPRENTICE`, o es `APPRENTICE` sin ficha activa, `chipName`/`chipCode`/`programName` llegan en `null` — el frontend decide cómo mostrarlo (por ejemplo, "Pendiente por ficha").
- Si no hay ningún usuario con sesión registrada, la respuesta es `200 OK` con `[]` (lista vacía, no un mensaje de error).

---

### 7.2 RF-10.2 — Consultar, editar, eliminar usuario

#### 7.2.1 Buscar por nombre o correo

**Endpoint**

```http
GET /api/admin/users/search?query={texto}
```

**Headers**

```text
Authorization: Bearer TOKEN_ADMIN_O_COORDINATOR
```

**Ejemplo**

```http
GET /api/admin/users/search?query=maria
```

**Respuesta esperada**

HTTP STATUS

```text
200 OK
```

Response

```json
[
  {
    "userId": "3f634480-63a4-427e-934b-5519f41a86b5",
    "firstName": "María",
    "lastName": "Gómez",
    "documentNumber": "1098765432",
    "documentType": "CITIZENSHIP CARD",
    "birthDate": "2005-02-20",
    "email": "maria@gmail.com",
    "role": "APPRENTICE",
    "accountStatus": "ACTIVE",
    "registrationDate": "2026-07-03T09:00:00",
    "chipName": "Ficha ADSO 2025",
    "chipCode": "A3F9K2M7",
    "programName": "ADSO",
    "hasSession": true
  }
]
```

**Sin resultados**

```http
GET /api/admin/users/search?query=noexiste123
```

HTTP STATUS

```text
400 BAD REQUEST
```

Response

```json
{
  "message": "No se encontraron usuarios con ese criterio"
}
```

**Notas**
- La búsqueda es parcial y no distingue mayúsculas/minúsculas — busca tanto en nombre completo (`firstName + lastName`) como en el correo.
- Igual que en el listado general, solo trae usuarios con al menos una sesión registrada.

---

#### 7.2.2 Ver detalle completo de un usuario

**Endpoint**

```http
GET /api/admin/users/{userId}
```

**Headers**

```text
Authorization: Bearer TOKEN_ADMIN_O_COORDINATOR
```

**Respuesta esperada**

HTTP STATUS

```text
200 OK
```

Response

```json
{
  "userId": "3f634480-63a4-427e-934b-5519f41a86b5",
  "firstName": "María",
  "lastName": "Gómez",
  "documentNumber": "1098765432",
  "documentType": "CITIZENSHIP CARD",
  "birthDate": "2005-02-20",
  "email": "maria@gmail.com",
  "role": "APPRENTICE",
  "accountStatus": "ACTIVE",
  "registrationDate": "2026-07-03T09:00:00",
  "chipName": "Ficha ADSO 2025",
  "chipCode": "A3F9K2M7",
  "programName": "ADSO",
  "hasSession": true
}
```

**Usuario inexistente**

HTTP STATUS

```text
400 BAD REQUEST
```

Response

```json
{
  "message": "Usuario no encontrado"
}
```

---

#### 7.2.3 Editar usuario (nombre, apellido, estado, rol)

**Endpoint**

```http
PUT /api/admin/users/{userId}
```

**Headers**

```text
Authorization: Bearer TOKEN_ADMIN_O_COORDINATOR
Content-Type: application/json
```

**JSON**

```json
{
  "firstName": "María José",
  "lastName": "Gómez Ruiz",
  "accountStatus": "ACTIVE",
  "role": "APPRENTICE"
}
```

**Valores permitidos**

| Campo | Valores |
|---|---|
| `accountStatus` | `ACTIVE`, `INACTIVE`, `PENDING_CONSENT`, `BLOCKED` |
| `role` | `APPRENTICE`, `INSTRUCTOR`, `ADMINISTRATOR`, `COORDINATOR` |

**Respuesta esperada**

HTTP STATUS

```text
200 OK
```

Response

```json
{
  "userId": "3f634480-63a4-427e-934b-5519f41a86b5",
  "firstName": "María José",
  "lastName": "Gómez Ruiz",
  "documentNumber": "1098765432",
  "documentType": "CITIZENSHIP CARD",
  "birthDate": "2005-02-20",
  "email": "maria@gmail.com",
  "role": "APPRENTICE",
  "accountStatus": "ACTIVE",
  "registrationDate": "2026-07-03T09:00:00",
  "chipName": "Ficha ADSO 2025",
  "chipCode": "A3F9K2M7",
  "programName": "ADSO",
  "hasSession": true
}
```

**Validaciones para probar**

| Caso | Respuesta esperada |
|---|---|
| Usuario inexistente | `400` — `"Usuario no encontrado"` |
| `firstName`/`lastName` vacíos | `400` — error de validación por campo (`MethodArgumentNotValidException`) |
| `firstName`/`lastName` con números o símbolos | `400` — `"El nombre solo puede contener letras"` / `"El apellido solo puede contener letras"` |
| `accountStatus` ausente | `400` — `"El estado de cuenta es obligatorio"` |
| `role` ausente | `400` — `"El rol es obligatorio"` |
| `role` con un valor que no existe en el enum | `400` — error de deserialización JSON |

> **Nota:** el correo electrónico y el número de documento **no** se pueden editar desde este endpoint — no existen como campos en el body, son de solo lectura por diseño.

---

#### 7.2.4 Eliminar usuario permanentemente

**Endpoint**

```http
DELETE /api/admin/users/{userId}
```

**Headers**

```text
Authorization: Bearer TOKEN_ADMIN_O_COORDINATOR
```

**Respuesta esperada**

HTTP STATUS

```text
204 NO CONTENT
```

Sin body.

> ⚠️ Esta operación borra en cascada: `user_app`, `credential`, `user_role`, `user_session`, `user_configuration`, `email_verification`, `password_recovery`, `consent`, `consent_verification`, `terms_acceptance`. Prueba primero con un usuario de prueba que no te importe perder.

**Validaciones para probar**

| Caso | Respuesta esperada |
|---|---|
| Usuario inexistente | `400` — `"Usuario no encontrado"` |
| Usuario con ficha activa (`user_chip` en estado `ACTIVE`) | `400` — `"No se puede eliminar porque está vinculado a una ficha activa. Desvincula primero al aprendiz."` |
| Usuario sin ficha activa y sin bloqueos | `204 NO CONTENT` |

---

### 7.3 Resumen de casos de prueba

| Caso | Resultado esperado |
|---|---|
| Listar usuarios con sesión | `200 OK` |
| Listar sin usuarios con sesión | `200 OK` con `[]` |
| Buscar por nombre/correo con coincidencias | `200 OK` |
| Buscar sin coincidencias | `400` — `"No se encontraron usuarios con ese criterio"` |
| Ver detalle de usuario existente | `200 OK` |
| Ver detalle de usuario inexistente | `400` — `"Usuario no encontrado"` |
| Editar usuario válido | `200 OK` |
| Editar usuario inexistente | `400` — `"Usuario no encontrado"` |
| Eliminar usuario sin bloqueos | `204 NO CONTENT` |
| Eliminar usuario con ficha activa | `400` — `"No se puede eliminar porque está vinculado a una ficha activa..."` |
| Sin token | `403 Forbidden` |
| Token de `INSTRUCTOR` o `APPRENTICE` | `403 Forbidden` |

---

### 7.4 Pendiente (no implementado todavía)

- Validación de "no eliminar si tiene asistencias registradas" — el módulo de asistencia (RF-6, reconocimiento facial) no existe aún.
- Registro en bitácora de consultas, ediciones y eliminaciones — módulo de auditoría pospuesto.
- Restricción de `accountStatus` a solo `ACTIVE`/`INACTIVE`/`BLOCKED` en la edición — actualmente el DTO acepta cualquier valor del enum `AccountStatus`, incluyendo `PENDING_CONSENT`, que el criterio de aceptación de RF-10.2 no contempla como estado editable manualmente.