# Roles y Service — Conceptos clave
**Proyecto:** FaceLit  
**Módulo:** auth  

---

## El Service no solo tiene `register`

El Service tiene todos los métodos que necesite ese módulo. Se van agregando progresivamente según la HU que se esté trabajando — no todos de una vez.

Cuando el módulo de usuario esté completo, el Service se verá así:

```java
public interface UserService {

    // HU-01 — registro de usuario
    RegisterResponseDTO register(RegisterRequestDTO dto);

    // HU futura — actualizar datos del usuario
    UserResponseDTO update(UUID id, UpdateUserRequestDTO dto);

    // HU futura — el admin ve todos los usuarios
    List<UserResponseDTO> findAll();

    // HU futura — buscar un usuario por id
    UserResponseDTO findById(UUID id);

    // HU futura — desactivar usuario (soft delete)
    void delete(UUID id);
}
```

**Regla:** solo se agrega un método al Service cuando hay una HU que lo necesita. No se anticipa código que todavía no tiene requerimiento.

---

## Los roles y cómo cambia el backend según el rol

El sistema tiene tres roles: **ADMIN**, **INSTRUCTOR**, **APRENDIZ**.

Según el rol, el backend restringe qué puede hacer cada quien. Esto se maneja en dos lugares.

---

### 1. En Spring Security — restricciones por endpoint

Define qué URLs puede tocar cada rol. Se configura en `SecurityConfig.java`:

```java
// Solo el ADMIN puede ver todos los usuarios
.requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")

// El INSTRUCTOR y el ADMIN pueden ver las fichas
.requestMatchers(HttpMethod.GET, "/api/fichas").hasAnyRole("ADMIN", "INSTRUCTOR")

// Cualquier usuario autenticado puede ver su propio perfil
.requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()

// Registro y login son públicos — nadie necesita token para registrarse
.requestMatchers("/api/auth/**").permitAll()
```

---

### 2. En el ServiceImpl — restricciones más finas

Cuando la restricción no es solo "puede o no puede entrar al endpoint" sino "puede ver solo sus propios datos", la lógica va en el Service:

```java
// Un instructor solo puede ver los aprendices de su propia ficha
public List<AprendizResponseDTO> findMyAprendices(UUID instructorId) {
    return aprendizRepository.findByInstructorId(instructorId);
}

// Un aprendiz solo puede ver sus propias asistencias
public List<AsistenciaResponseDTO> findMyAsistencias(UUID aprendizId) {
    return asistenciaRepository.findByAprendizId(aprendizId);
}
```

---

## Cómo viaja el rol en cada request

El rol no se consulta en BD en cada request. Viaja dentro del **JWT** que se genera al hacer login.

```
Token JWT contiene adentro:
{
  "userId": "uuid-aqui",
  "email": "usuario@email.com",
  "role": "INSTRUCTOR",
  "exp": 1234567890
}
```

Spring Security lee ese token automáticamente en cada request y sabe quién es el usuario y qué rol tiene. Con eso aplica las restricciones definidas en `SecurityConfig`.

---

## División de trabajo — BD vs Backend

| Tu amiga (BD) | Tú (Backend) |
|---|---|
| Crea tabla `role` con los roles del sistema | Lees el rol del usuario del JWT en cada request |
| Agrega columna `role_id` en tabla `user` | Defines qué endpoints puede usar cada rol en SecurityConfig |
| Define relaciones entre tablas | Filtras los datos según el rol en el ServiceImpl |

---

## Cuándo se implementa esto

El manejo de roles se implementa cuando llegues a la HU del **login**. Ahí es donde:

1. El usuario manda email y contraseña
2. El backend verifica las credenciales
3. El backend lee el rol del usuario desde BD
4. El backend genera el JWT con el rol incluido
5. A partir de ahí cada request lleva el rol en el token

Por ahora en HU-01 de registro no hay JWT ni roles — el endpoint de registro es público. Cuando llegues a la HU de login se implementa todo esto.

---

*Documento del equipo FaceLit — para tener en cuenta en HUs futuras*