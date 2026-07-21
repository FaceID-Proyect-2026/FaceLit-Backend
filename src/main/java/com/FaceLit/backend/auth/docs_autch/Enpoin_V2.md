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