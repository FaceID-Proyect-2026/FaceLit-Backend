# FaceLit - Endpoints del modulo 1: Acceso al sistema V3

## Base URL

```text
http://localhost:8080
```

Todos los errores de negocio responden `{ "message": "..." }`.

## Pantallas publicas

### 1. Inicio de sesion

`POST /api/auth/login`

Body inicial, cuando el usuario aun no ha aceptado privacidad:

```json
{
  "documentNumber": "1234567890",
  "password": "Maria@123",
  "accepted": true
}
```

Body si el usuario ya acepto privacidad:

```json
{
  "documentNumber": "1234567890",
  "password": "Maria@123",
  "accepted": true
}
```

El backend busca por `user_app.number_document`, nunca por correo. El correo no es un identificador valido para este endpoint.

Respuesta `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "INSTRUCTOR",
  "permissions": [
    "VIEW_OWN_PROFILE",
    "VIEW_OWN_ATTENDANCE",
    "VIEW_FICHA_ATTENDANCE"
  ],
  "userId": "UUID_DEL_USUARIO",
  "message": "Inicio de sesión exitoso"
}
```

Headers para cualquier pantalla protegida:

```http
Authorization: Bearer TOKEN_RECIBIDO
```

Casos de prueba:

```json
{
  "documentNumber": "9999999999",
  "password": "Incorrecta@123",
  "accepted": true
}
```

Respuesta: `401 Unauthorized`, `{ "message": "Documento o contraseña incorrectos" }`.

Si no acepta privacidad durante el primer acceso:

```json
{
  "documentNumber": "1234567890",
  "password": "Maria@123",
  "accepted": false
}
```

Respuesta: `401 Unauthorized`, `{ "message": "Políticas no aceptadas" }`.

Si la cuenta esta inactiva: `{ "message": "La cuenta no está activa. Contacta al Coordinador" }`.

Despues de cinco intentos fallidos consecutivos, la credencial queda bloqueada durante 15 minutos y responde:

```json
{ "message": "La cuenta está temporalmente bloqueada. Intenta más tarde o recupera tu contraseña" }
```

### 2. Solicitar recuperacion de contraseña

`POST /api/auth/request-recovery`

El correo se usa exclusivamente en esta pantalla.

```json
{
  "email": "maria@gmail.com"
}
```

Respuesta `200 OK`:

```json
{
  "message": "Se envió un código de recuperación a tu correo electrónico"
}
```

El mismo mensaje se devuelve aunque el correo no exista, para no revelar cuentas registradas. Si existe, el código se envia al correo y vence en 5 minutos.

### 3. Restablecer contraseña

`POST /api/auth/reset-password`

```json
{
  "token": "483920",
  "newPassword": "Nueva@123",
  "confirmPassword": "Nueva@123"
}
```

Respuesta `200 OK`:

```json
{
  "message": "Contraseña restablecida correctamente"
}
```

La contraseña debe tener entre 8 y 15 caracteres, una mayuscula, un numero y un simbolo. El token es de un solo uso. Los errores posibles son:

- `Código incorrecto`
- `Código vencido`
- `Las contraseñas no coinciden`
- `La contraseña debe tener entre 8 y 15 caracteres, una mayúscula, un número y un símbolo`

Una recuperacion correcta reinicia intentos fallidos y desbloquea la credencial.

## Pantallas protegidas por rol

### 4. Mi perfil

`GET /api/profile/me`

Header:

```http
Authorization: Bearer TOKEN_RECIBIDO
```

El usuario autenticado solo puede consultar su propio perfil.

### 5. Configuracion personal

Crear: `POST /api/profile/configuration`

Actualizar: `PUT /api/profile/configuration`

Consultar: `GET /api/profile/configuration`

Body:

```json
{
  "notificationsActive": true,
  "darkMode": false,
  "language": "ES"
}
```

Valores de `language`: `ES`, `EN`, `PR`, `FR`.

## Pantallas administrativas del Coordinador

Todas requieren:

```http
Authorization: Bearer TOKEN_DEL_COORDINADOR
```

### 6. Crear usuario

`POST /api/admin/users`

La creación de cuentas ya no es pública. El Coordinador crea la cuenta y el usuario inicia sesión posteriormente con el documento y la contraseña entregada por el Coordinador.

```json
{
  "firstName": "Maria",
  "lastName": "Gomez",
  "documentNumber": "1234567890",
  "email": "maria@gmail.com"
}
```

Respuesta `201 Created`:

```json
{
  "message": "Registro exitoso",
  "status": "REGISTERED",
  "requiresConsent": false,
  "id_user": "UUID_DEL_USUARIO",
  "temporaryPassword": "A7m#q2Pz9Lx!"
}
```

El usuario se crea `ACTIVE` con rol inicial `APPRENTICE`. El Coordinador puede asignar después `INSTRUCTOR` o `COORDINATOR`. La aceptación de privacidad se solicita y guarda en el primer login, no durante la creación administrativa.

### 6. Listar usuarios

`GET /api/admin/users`

Solo devuelve usuarios con al menos una sesion registrada.

### 7. Buscar usuarios

`GET /api/admin/users/search?query=maria`

Busca por nombre completo o correo. El correo solo se muestra para administración y recuperación; no funciona como login.

### 8. Ver detalle

`GET /api/admin/users/{userId}`

### 9. Actualizar datos y rol

`PUT /api/admin/users/{userId}`

```json
{
  "firstName": "Maria Jose",
  "lastName": "Gomez Ruiz",
  "accountStatus": "ACTIVE",
  "role": "APPRENTICE"
}
```

Solo el Coordinador puede modificar datos personales y roles. Aprendiz e Instructor no tienen endpoint de edición personal.

### 10. Eliminar usuario

`DELETE /api/admin/users/{userId}`

Respuesta correcta: `204 No Content`. No permite eliminar un aprendiz con `user_chip` activo.

### 11. Cambiar solo el rol

`PUT /api/admin/users/{userId}/role`

```json
{
  "role": "INSTRUCTOR"
}
```

El backend elimina la asignación anterior y aplica los permisos del nuevo rol desde `role_permission`. El JWT conserva la identidad, pero el filtro revalida rol y permisos actuales desde la base.

## Reglas de seguridad implementadas

- Login exclusivamente por numero de documento.
- Correo reservado para recuperacion de contraseña.
- Contraseñas almacenadas con BCrypt.
- JWT firmado y enviado como `Bearer`.
- Rol y permisos consultados desde `user_role` y `role_permission`.
- Aceptacion de privacidad guardada en `legal.terms_acceptance` una sola vez.
- Bloqueo temporal despues de cinco intentos fallidos.
- Recuperacion con codigo de un solo uso y vencimiento de cinco minutos.
- Gestion administrativa separada de autenticacion y autorizacion.

## Flujo rapido para Thunder Client

1. Ejecuta `POST /api/auth/login` con documento, contraseña y `accepted: true`.
2. Copia `token` de la respuesta.
3. Crea una variable de entorno `token` con ese valor.
4. En endpoints protegidos usa `Authorization: Bearer {{token}}`.
5. Para probar recuperación, ejecuta `POST /api/auth/request-recovery` con el correo y copia el codigo recibido.
6. Ejecuta `POST /api/auth/reset-password` con el codigo y la nueva contraseña.
