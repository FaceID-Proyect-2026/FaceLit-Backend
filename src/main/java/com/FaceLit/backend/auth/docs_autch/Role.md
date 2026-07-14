# Documentacion - Sistema de Roles y Permisos (RBAC)
## FaceLit Backend

---

## Que es RBAC y por que se uso

RBAC (Role-Based Access Control) es un patron de seguridad donde los permisos no se asignan directamente a los usuarios, sino a los roles, y los roles se asignan a los usuarios.

```
Usuario -> tiene un Rol -> el Rol tiene Permisos -> los Permisos definen que puede hacer
```

---

## Los cuatro roles del sistema

| Rol | Descripcion |
|---|---|
| ADMINISTRATOR | Acceso total. Hace todo lo que hace COORDINATOR y ademas gestiona y repara el sistema. |
| COORDINATOR | Gestiona usuarios y asigna roles. Ve todos los usuarios y todas las asistencias. |
| INSTRUCTOR | Acceso medio. Ve solo sus fichas, sus programas y sus propios datos. |
| APPRENTICE | Acceso minimo. Solo ve sus propios datos y su propia asistencia. |

Jerarquia: ADMINISTRATOR hereda TODOS los permisos de COORDINATOR. No existen permisos exclusivos nuevos para ADMINISTRATOR, tiene la misma matriz de permisos que COORDINATOR.

---

## Matriz de permisos

| Permiso | ADMINISTRATOR | COORDINATOR | INSTRUCTOR | APPRENTICE |
|---|---|---|---|---|
| VIEW_OWN_PROFILE | SI | SI | SI | SI |
| EDIT_OWN_PROFILE | SI | SI | SI | SI |
| VIEW_ALL_USERS | SI | SI | NO | NO |
| MANAGE_USERS | SI | SI | NO | NO |
| ASSIGN_ROLES | SI | SI | NO | NO |
| VIEW_ALL_ATTENDANCE | SI | SI | NO | NO |
| VIEW_OWN_ATTENDANCE | SI | SI | SI | SI |
| VIEW_FICHA_ATTENDANCE | SI | SI | SI | NO |
| MANAGE_SCHEDULES | SI | SI | SI | NO |

---

## Esquema de base de datos - roleandpermission

### Tabla role
```sql
INSERT INTO roleandpermission.role (id_role, name_role, created_at)
VALUES
    (uuid_generate_v4(), 'ADMINISTRATOR', NOW()),
    (uuid_generate_v4(), 'COORDINATOR', NOW()),
    (uuid_generate_v4(), 'INSTRUCTOR', NOW()),
    (uuid_generate_v4(), 'APPRENTICE', NOW());
```

### Tabla permission
Mismos 9 permisos de siempre, no se agregaron nuevos.
```sql
INSERT INTO roleandpermission.permission (id_permission, name_permission, description, created_at)
VALUES
    (uuid_generate_v4(), 'VIEW_OWN_PROFILE',      'Ver su propio perfil',             NOW()),
    (uuid_generate_v4(), 'EDIT_OWN_PROFILE',      'Editar su propio perfil',          NOW()),
    (uuid_generate_v4(), 'VIEW_ALL_USERS',         'Ver todos los usuarios',           NOW()),
    (uuid_generate_v4(), 'MANAGE_USERS',           'Gestionar usuarios',               NOW()),
    (uuid_generate_v4(), 'ASSIGN_ROLES',           'Asignar roles a usuarios',         NOW()),
    (uuid_generate_v4(), 'VIEW_ALL_ATTENDANCE',    'Ver asistencia de todos',          NOW()),
    (uuid_generate_v4(), 'VIEW_OWN_ATTENDANCE',    'Ver su propia asistencia',         NOW()),
    (uuid_generate_v4(), 'VIEW_FICHA_ATTENDANCE',  'Ver asistencia de su ficha',       NOW()),
    (uuid_generate_v4(), 'MANAGE_SCHEDULES',       'Gestionar horarios',               NOW());
```

### Tabla role_permission
```sql
-- ADMINISTRATOR: todos los permisos (igual que COORDINATOR)
INSERT INTO roleandpermission.role_permission (id_role_permission, id_role, id_permission, assignment_date, assigned_at, created_at)
SELECT uuid_generate_v4(), r.id_role, p.id_permission, NOW(), NOW(), NOW()
FROM roleandpermission.role r
CROSS JOIN roleandpermission.permission p
WHERE r.name_role = 'ADMINISTRATOR';

-- COORDINATOR: todos los permisos
INSERT INTO roleandpermission.role_permission (id_role_permission, id_role, id_permission, assignment_date, assigned_at, created_at)
SELECT uuid_generate_v4(), r.id_role, p.id_permission, NOW(), NOW(), NOW()
FROM roleandpermission.role r
CROSS JOIN roleandpermission.permission p
WHERE r.name_role = 'COORDINATOR';

-- INSTRUCTOR: 5 permisos
INSERT INTO roleandpermission.role_permission (id_role_permission, id_role, id_permission, assignment_date, assigned_at, created_at)
SELECT uuid_generate_v4(), r.id_role, p.id_permission, NOW(), NOW(), NOW()
FROM roleandpermission.role r
CROSS JOIN roleandpermission.permission p
WHERE r.name_role = 'INSTRUCTOR'
AND p.name_permission IN (
    'VIEW_OWN_PROFILE', 'EDIT_OWN_PROFILE',
    'VIEW_OWN_ATTENDANCE', 'VIEW_FICHA_ATTENDANCE', 'MANAGE_SCHEDULES'
);

-- APPRENTICE: 3 permisos
INSERT INTO roleandpermission.role_permission (id_role_permission, id_role, id_permission, assignment_date, assigned_at, created_at)
SELECT uuid_generate_v4(), r.id_role, p.id_permission, NOW(), NOW(), NOW()
FROM roleandpermission.role r
CROSS JOIN roleandpermission.permission p
WHERE r.name_role = 'APPRENTICE'
AND p.name_permission IN (
    'VIEW_OWN_PROFILE', 'EDIT_OWN_PROFILE', 'VIEW_OWN_ATTENDANCE'
);
```

### Tabla user_role
Relaciona cada usuario con su rol. Se llena automaticamente al registrarse (APPRENTICE por defecto) y el ADMINISTRATOR/COORDINATOR la actualiza cuando cambia un rol.

---

## Cambio de codigo necesario - RoleName.java

```java
package com.FaceLit.backend.auth.model.enums;

public enum RoleName {
    APPRENTICE,
    INSTRUCTOR,
    COORDINATOR,
    ADMINISTRATOR
}
```

Si existe una constraint CHECK en la columna name_role de PostgreSQL, debe actualizarse:
```sql
ALTER TABLE roleandpermission.role DROP CONSTRAINT IF EXISTS role_name_role_check;
ALTER TABLE roleandpermission.role ADD CONSTRAINT role_name_role_check
    CHECK (name_role IN ('APPRENTICE', 'INSTRUCTOR', 'COORDINATOR', 'ADMINISTRATOR'));
```

---

## Cambio de codigo necesario - SecurityConfig.java

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/consent/**").permitAll()
    .requestMatchers("/api/catalogos/**").permitAll()

    // Gestion de usuarios y roles - ADMINISTRATOR y COORDINATOR
    .requestMatchers("/api/admin/**").hasAnyRole("ADMINISTRATOR", "COORDINATOR")

    // Asistencia de fichas, horarios
    .requestMatchers("/api/instructor/**").hasAnyRole("ADMINISTRATOR", "COORDINATOR", "INSTRUCTOR")

    // Todos los roles autenticados
    .requestMatchers("/api/apprentice/**").hasAnyRole("ADMINISTRATOR", "COORDINATOR", "INSTRUCTOR", "APPRENTICE")

    .anyRequest().authenticated())
```

---

## Flujo completo de roles

### Al registrarse
```
Usuario se registra
        ->
RegisterServiceImpl asigna APPRENTICE automaticamente
        ->
Se inserta en user_role: { id_user, id_role_apprentice }
        ->
Usuario hace login con rol APPRENTICE
```

### Al hacer login
```
LoginServiceImpl busca Credential por email
        ->
Busca UserRole del usuario -> obtiene Role
        ->
Busca RolePermission del rol -> obtiene lista de permisos
        ->
JwtServiceImpl genera JWT con:
{
  "userId": "uuid",
  "email": "maria@gmail.com",
  "role": "COORDINATOR",
  "permissions": ["VIEW_OWN_PROFILE", "VIEW_ALL_USERS", "ASSIGN_ROLES"],
  "exp": 1234596690
}
        ->
Frontend guarda el token en memoria
```

### En cada request protegido
```
Frontend manda: Authorization: Bearer eyJhbGci...
        ->
JwtFilter intercepta el request
        ->
Valida firma del token con JWT_SECRET
        ->
Extrae role -> construye ROLE_COORDINATOR
Extrae permissions -> construye lista de autoridades
        ->
Carga contexto de seguridad (SecurityContextHolder)
        ->
SecurityConfig verifica si el endpoint permite ese rol
        ->
Permite -> llega al Controller
No permite -> 403 Forbidden
```

### COORDINATOR o ADMINISTRATOR cambian un rol
```
COORDINATOR o ADMINISTRATOR hace login -> obtiene JWT con su rol
        ->
PUT /api/admin/users/{userId}/role { "role": "INSTRUCTOR" }
Authorization: Bearer TOKEN
        ->
AdminRoleServiceImpl busca el usuario por UUID
        ->
Busca el rol INSTRUCTOR en la tabla role
        ->
Actualiza user_role: { id_user, id_role_instructor }
        ->
Proximo login del usuario -> JWT lleva INSTRUCTOR y sus 5 permisos
```

Tanto ADMINISTRATOR como COORDINATOR pueden ejecutar este endpoint, ya que /api/admin/** ahora acepta ambos roles.

---

## Archivos creados o modificados por capa

### auth/model/enums/

RoleName.java (modificado):
```java
APPRENTICE
INSTRUCTOR
COORDINATOR
ADMINISTRATOR
```

### auth/model/roleandpermission/

- Role.java: sin cambios, el enum es el que cambio
- Permission.java: sin cambios, los permisos siguen siendo los mismos 9
- UserRole.java: sin cambios estructurales
- RolePermission.java: sin cambios estructurales, solo se agregaron mas filas para COORDINATOR

### auth/repository/roleandpermission/

Sin cambios. Los metodos siguen funcionando porque trabajan sobre el enum RoleName, que ya incluye COORDINATOR.

```java
RoleRepository.findByNameRole(RoleName nameRole)
// Usado en: RegisterServiceImpl, AdminRoleServiceImpl

UserRoleRepository.findByUserId(UUID idUser)
// Usado en: LoginServiceImpl, AdminRoleServiceImpl

RolePermissionRepository.findByRole_IdRole(UUID idRole)
// Usado en: LoginServiceImpl

PermissionRepository
// Hereda metodos de JpaRepository, sin cambios
```

### auth/dto/request/roleandpermission/

AssignRoleRequestDTO.java: sin cambios de estructura, ahora simplemente acepta tambien el valor "COORDINATOR".
```json
{ "role": "COORDINATOR" }
```

### auth/dto/response/roleandpermission/

AssignRoleResponseDTO.java: sin cambios.
```json
{
  "userId": "uuid-del-usuario",
  "role": "COORDINATOR",
  "message": "Rol asignado correctamente"
}
```

LoginResponseDTO.java: sin cambios, el campo role ahora puede traer tambien "COORDINATOR".

### auth/dto/response/security/

UserListResponseDTO.java: sin cambios.
```json
{
  "userId": "uuid",
  "firstName": "Maria",
  "lastName": "Oyola",
  "email": "maria@gmail.com",
  "documentNumber": "1234567890",
  "currentRole": "COORDINATOR"
}
```

### auth/service/roleandpermission/

AdminRoleService.java (interfaz): sin cambios de firma.
```java
List<UserListResponseDTO> getAllUsers()
AssignRoleResponseDTO assignRole(UUID userId, AssignRoleRequestDTO dto)
```

JwtService.java / LoginService.java: sin cambios.

### auth/service/serviceImpl/roleandpermission/

AdminRoleServiceImpl.java: sin cambios de logica, la implementacion es generica respecto al RoleName, asi que funciona igual con 4 roles que con 3.

```
getAllUsers():
1. Trae todos los User de BD
2. Por cada usuario: obtiene email y rol actual
3. Retorna lista de UserListResponseDTO

assignRole():
1. Verifica que el usuario existe
2. Busca el rol nuevo, ahora puede ser COORDINATOR
3. Actualiza o crea el UserRole
4. Retorna AssignRoleResponseDTO.success()
```

LoginServiceImpl.java / JwtServiceImpl.java: sin cambios.

### auth/controller/roleandpermission/

AdminRoleController.java: sin cambios de codigo, la proteccion de la ruta se actualiza en SecurityConfig, no en el controller.
```
GET /api/admin/users              -> lista usuarios con rol actual
PUT /api/admin/users/{id}/role    -> cambia el rol de un usuario
```
Ahora accesible por ADMINISTRATOR y COORDINATOR.

### config/

JwtFilter.java: sin cambios, funciona igual independientemente de cuantos roles existan.

SecurityConfig.java (modificado):
```java
// Antes:
.requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
.requestMatchers("/api/instructor/**").hasAnyRole("ADMINISTRATOR", "INSTRUCTOR")

// Ahora:
.requestMatchers("/api/admin/**").hasAnyRole("ADMINISTRATOR", "COORDINATOR")
.requestMatchers("/api/instructor/**").hasAnyRole("ADMINISTRATOR", "COORDINATOR", "INSTRUCTOR")
```

---

## Por que el JWT no se guarda en BD

El JWT es stateless, no necesita guardarse porque:

- El token lleva dentro toda la informacion necesaria (userId, role, permissions)
- Esta firmado con JWT_SECRET, si alguien lo modifica, la firma no coincide y se rechaza
- El backend solo verifica la firma en cada request, sin consultar BD

Lo que SI se guarda en BD es la sesion en security.user_session:
```
start_date     -> cuando inicio sesion
end_date       -> cuando cerro sesion (null hasta el logout)
session_status -> ACTIVE o INACTIVE
```

---

## Endpoints para probar en Postman

### 1. Login como ADMINISTRATOR o COORDINATOR
```http
POST /api/auth/login
```
```json
{
  "email": "coordinador@gmail.com",
  "password": "Coord@123",
  "aceptoPoliticas": true
}
```

### 2. Ver todos los usuarios
```http
GET /api/admin/users
Authorization: Bearer TOKEN_DEL_COORDINATOR_O_ADMIN
```

### 3. Cambiar rol de un usuario
```http
PUT /api/admin/users/{userId}/role
Authorization: Bearer TOKEN_DEL_COORDINATOR_O_ADMIN
Content-Type: application/json
```
```json
{ "role": "COORDINATOR" }
```

### Validaciones a probar

| Caso | Respuesta esperada |
|---|---|
| Sin token en /api/admin/** | 403 Forbidden |
| Token de APPRENTICE en /api/admin/** | 403 Forbidden |
| Token de INSTRUCTOR en /api/admin/** | 403 Forbidden |
| Token de COORDINATOR en /api/admin/** | 200 OK (nuevo) |
| Token de ADMINISTRATOR en /api/admin/** | 200 OK |
| UUID de usuario inexistente | "Usuario no encontrado" |
| Rol invalido ("SUPERADMIN") | 400 Bad Request |
| Asignar rol "COORDINATOR" a un usuario | 200 OK con role COORDINATOR (nuevo) |

---

## Checklist de cambios pendientes para implementar la actualizacion

- [ ] Actualizar RoleName.java agregando COORDINATOR
- [ ] Insertar el nuevo rol COORDINATOR en BD (seed)
- [ ] Insertar los role_permission de COORDINATOR (mismos 9 permisos que ADMINISTRATOR)
- [ ] Actualizar la constraint CHECK de la tabla role en PostgreSQL si existe
- [ ] Actualizar SecurityConfig.java para que /api/admin/** acepte ADMINISTRATOR y COORDINATOR
- [ ] Actualizar /api/instructor/** para incluir COORDINATOR en la cadena de roles permitidos
- [ ] RegisterServiceImpl sigue asignando APPRENTICE por defecto, sin cambios necesarios aqui
- [ ] Probar en Postman: login con COORDINATOR, verificar permissions, probar /api/admin/users con ambos roles de gestion