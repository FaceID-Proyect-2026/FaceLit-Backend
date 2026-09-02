# RF-10 - Gestion de usuarios

Documento generado a partir del codigo vigente del backend para que Front pueda integrar el modulo.

## 1. Ubicacion del modulo

La implementacion principal esta en:

```text
src/main/java/com/FaceLit/backend/auth/
├── controller/
│   ├── security/UserManagementController.java
│   └── roleandpermission/AdminRoleController.java
├── dto/
│   ├── request/security/UpdateUserRequestDTO.java
│   ├── request/roleandpermission/AssignRoleRequestDTO.java
│   └── response/security/UserDetailResponseDTO.java
├── exception/UserManagementException.java
├── model/
│   ├── enums/AccountStatus.java
│   ├── enums/RoleName.java
│   └── security/User.java
├── repository/
│   ├── security/UserRepository.java
│   └── roleandpermission/UserRoleRepository.java
└── service/
    ├── security/UserManagementService.java
    ├── serviceImpl/security/UserManagementServiceImpl.java
    └── serviceImpl/roleandpermission/AdminRoleServiceImpl.java
```

Archivos transversales usados por el modulo:

```text
src/main/java/com/FaceLit/backend/config/SecurityConfig.java
src/main/java/com/FaceLit/backend/shared/exception/GlobalExceptionHandler.java
```

**URL base:** `/api/admin/users`

**Autorizacion:** todos los endpoints de RF-10 requieren JWT y uno de estos roles:
`ADMINISTRATOR` o `COORDINATOR`.

Enviar el token asi:

```http
Authorization: Bearer <JWT>
```

## 2. RF-10.1 - Panel de usuarios registrados

### Listar usuarios

```http
GET /api/admin/users
```

La respuesta contiene un arreglo. Actualmente solo incluye usuarios que han iniciado sesion al menos una vez.

Respuesta `200 OK`:

```json
[
  {
    "userId": "8d2d4d36-6f22-4b5f-9c5b-4a7fc2b4e111",
    "firstName": "Maria",
    "lastName": "Gomez",
    "documentNumber": "100200300",
    "documentType": "Cedula de ciudadania",
    "birthDate": "2005-04-12",
    "email": "maria@example.com",
    "role": "APPRENTICE",
    "accountStatus": "ACTIVE",
    "registrationDate": "2026-08-20T14:30:00",
    "chipName": "Ficha Desarrollo Web",
    "chipCode": "DW-01",
    "programName": "Analisis y desarrollo de software",
    "hasSession": true
  }
]
```

Notas:

- `chipName`, `chipCode` y `programName` pueden ser `null` si no existe una ficha activa.
- Si el usuario es aprendiz y no tiene ficha, Front puede mostrar `Pendiente por ficha`.
- `hasSession` es booleano y para esta lista sera `true`.
- `registrationDate` corresponde a la fecha de auditoria de creacion del usuario.
- La lista no tiene paginacion ni parametros adicionales actualmente.

## 3. RF-10.2 - Consultar por nombre o correo

### Buscar usuarios

```http
GET /api/admin/users/search?query=<texto>
```

La busqueda es parcial, ignora mayusculas/minusculas y consulta:

- nombre completo: `firstName + lastName`
- correo almacenado en `Credential.email`

Ejemplo:

```http
GET /api/admin/users/search?query=maria
```

Respuesta `200 OK`: usa el mismo formato de `UserDetailResponseDTO` mostrado en el listado.

Si `query` es vacio, solo espacios o no hay resultados:

```http
400 Bad Request
Content-Type: application/json

{
  "message": "No se encontraron usuarios con ese criterio"
}
```

## 4. RF-10.2 - Ver detalle

```http
GET /api/admin/users/{userId}
```

Ejemplo:

```http
GET /api/admin/users/8d2d4d36-6f22-4b5f-9c5b-4a7fc2b4e111
```

Respuesta `200 OK`:

```json
{
  "userId": "8d2d4d36-6f22-4b5f-9c5b-4a7fc2b4e111",
  "firstName": "Maria",
  "lastName": "Gomez",
  "documentNumber": "100200300",
  "documentType": "Cedula de ciudadania",
  "birthDate": "2005-04-12",
  "email": "maria@example.com",
  "role": "APPRENTICE",
  "accountStatus": "ACTIVE",
  "registrationDate": "2026-08-20T14:30:00",
  "chipName": null,
  "chipCode": null,
  "programName": null,
  "hasSession": true
}
```

Si el UUID no existe:

```http
400 Bad Request

{
  "message": "Usuario no encontrado"
}
```

## 5. RF-10.2 - Editar usuario

```http
PUT /api/admin/users/{userId}
Content-Type: application/json
```

El body requiere todos los campos siguientes:

```json
{
  "firstName": "Maria",
  "lastName": "Gomez",
  "accountStatus": "ACTIVE",
  "role": "INSTRUCTOR"
}
```

Campos permitidos:

| Campo | Tipo | Obligatorio | Validacion / valores |
|---|---|---:|---|
| `firstName` | string | Si | No vacio, maximo 50 caracteres, solo letras, espacios y letras espanolas |
| `lastName` | string | Si | No vacio, maximo 50 caracteres, solo letras, espacios y letras espanolas |
| `accountStatus` | enum | Si | `ACTIVE`, `INACTIVE`, `PENDING_CONSENT`, `BLOCKED` |
| `role` | enum | Si | `APPRENTICE`, `INSTRUCTOR`, `ADMINISTRATOR`, `COORDINATOR` |

Aunque el criterio funcional suele presentar `ACTIVE`, `INACTIVE` y `BLOCKED`, el DTO actual acepta tambien `PENDING_CONSENT` porque pertenece al enum `AccountStatus`.

Respuesta `200 OK`: devuelve el usuario actualizado con el mismo formato de detalle.

La operacion actualiza nombre, apellido y estado en `user_app`, y asigna o reemplaza el rol relacionado en `user_role`.

Error de validacion:

```http
400 Bad Request

{
  "firstName": "El nombre es obligatorio",
  "role": "El rol es obligatorio"
}
```

Los campos que no fallen no aparecen en el objeto de error. El backend usa un mapa campo-mensaje.

## 6. RF-10.2 - Eliminar usuario

```http
DELETE /api/admin/users/{userId}
```

Respuesta exitosa:

```http
204 No Content
```

Reglas actuales:

- Si el usuario no existe: `400` con `{"message":"Usuario no encontrado"}`.
- Si tiene una ficha activa: `400` con:

```json
{
  "message": "No se puede eliminar porque está vinculado a una ficha activa. Desvincula primero al aprendiz."
}
```

- Si no tiene ficha activa, se eliminan sus dependencias en este orden: consentimiento y verificacion, aceptacion de terminos, recuperaciones de contrasena, sesiones, verificaciones de correo, configuracion, rol, credencial y finalmente el usuario.
- Las asistencias aun no se validan porque el modulo de asistencia no existe en este momento.
- El guardian no se elimina desde esta operacion porque puede estar asociado a otro hermano.

## 7. Endpoint de roles existente

Ademas del endpoint de edicion de RF-10, permanece disponible el endpoint historico para cambiar solamente el rol:

```http
PUT /api/admin/users/{userId}/role
Content-Type: application/json
```

Body:

```json
{
  "role": "INSTRUCTOR"
}
```

Respuesta actual:

```json
{
  "userId": "8d2d4d36-6f22-4b5f-9c5b-4a7fc2b4e111",
  "role": "INSTRUCTOR",
  "message": "Rol asignado correctamente"
}
```

Para la pantalla de edicion de RF-10 debe preferirse `PUT /api/admin/users/{userId}`, porque permite actualizar todos los campos editables en una sola solicitud.

## 8. Contratos Java actuales

### `auth/controller/security/UserManagementController.java`

```java
package com.FaceLit.backend.auth.controller.security;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.FaceLit.backend.auth.dto.request.security.UpdateUserRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.UserDetailResponseDTO;
import com.FaceLit.backend.auth.service.security.UserManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ResponseEntity<List<UserDetailResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDetailResponseDTO>> search(
            @RequestParam String query) {
        return ResponseEntity.ok(userManagementService.searchUsers(query));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponseDTO> getDetail(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(userManagementService.getUserDetail(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDetailResponseDTO> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequestDTO dto) {
        return ResponseEntity.ok(userManagementService.updateUser(userId, dto));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
```

### `auth/dto/request/security/UpdateUserRequestDTO.java`

```java
package com.FaceLit.backend.auth.dto.request.security;

import com.FaceLit.backend.auth.model.enums.AccountStatus;
import com.FaceLit.backend.auth.model.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo puede contener letras")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El apellido solo puede contener letras")
    @Size(max = 50, message = "El apellido no puede superar los 50 caracteres")
    private String lastName;

    @NotNull(message = "El estado de cuenta es obligatorio")
    private AccountStatus accountStatus;

    @NotNull(message = "El rol es obligatorio")
    private RoleName role;
}
```

### `auth/dto/response/security/UserDetailResponseDTO.java`

```java
package com.FaceLit.backend.auth.dto.response.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDetailResponseDTO {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String documentType;
    private LocalDate birthDate;
    private String email;
    private String role;
    private String accountStatus;
    private LocalDateTime registrationDate;
    private String chipName;
    private String chipCode;
    private String programName;
    private boolean hasSession;
}
```

### `auth/service/security/UserManagementService.java`

```java
package com.FaceLit.backend.auth.service.security;

import java.util.List;
import java.util.UUID;
import com.FaceLit.backend.auth.dto.request.security.UpdateUserRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.UserDetailResponseDTO;

public interface UserManagementService {

    List<UserDetailResponseDTO> getAllUsers();
    List<UserDetailResponseDTO> searchUsers(String query);
    UserDetailResponseDTO getUserDetail(UUID userId);
    UserDetailResponseDTO updateUser(UUID userId, UpdateUserRequestDTO dto);
    void deleteUser(UUID userId);
}
```

### `auth/model/enums/AccountStatus.java`

```java
package com.FaceLit.backend.auth.model.enums;

public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    PENDING_CONSENT,
    BLOCKED
}
```

### `auth/model/enums/RoleName.java`

```java
package com.FaceLit.backend.auth.model.enums;

public enum RoleName {
    APPRENTICE,
    INSTRUCTOR,
    ADMINISTRATOR,
    COORDINATOR
}
```

## 9. Implementacion actual del servicio

Ruta: `auth/service/serviceImpl/security/UserManagementServiceImpl.java`

```java
package com.FaceLit.backend.auth.service.serviceImpl.security;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.dto.request.roleandpermission.AssignRoleRequestDTO;
import com.FaceLit.backend.auth.dto.request.security.UpdateUserRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.UserDetailResponseDTO;
import com.FaceLit.backend.auth.exception.UserManagementException;
import com.FaceLit.backend.academic.model.enums.UserChipStatus;
import com.FaceLit.backend.auth.model.security.Credential;
import com.FaceLit.backend.auth.model.security.EmailVerification;
import com.FaceLit.backend.auth.model.security.PasswordRecovery;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.model.security.UserSession;
import com.FaceLit.backend.auth.repository.legal.AcceptanceTermsRepository;
import com.FaceLit.backend.auth.repository.legal.ConsentRepository;
import com.FaceLit.backend.auth.repository.legal.ConsentVerificationRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.EmailVerificationRepository;
import com.FaceLit.backend.auth.repository.security.PasswordRecoveryRepository;
import com.FaceLit.backend.auth.repository.security.UserConfigurationRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.repository.security.UserSessionRepository;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;
import com.FaceLit.backend.auth.service.security.UserManagementService;
import com.FaceLit.backend.academic.repository.academic.UserChipRepository;
import com.FaceLit.backend.academic.model.academic.UserChip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserConfigurationRepository userConfigurationRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserChipRepository userChipRepository;
    private final AdminRoleService adminRoleService;
    private final ConsentRepository consentRepository;
    private final ConsentVerificationRepository consentVerificationRepository;
    private final AcceptanceTermsRepository acceptanceTermsRepository;
    private final PasswordRecoveryRepository passwordRecoveryRepository;

    public UserManagementServiceImpl(
            UserRepository userRepository,
            CredentialRepository credentialRepository,
            UserRoleRepository userRoleRepository,
            UserSessionRepository userSessionRepository,
            UserConfigurationRepository userConfigurationRepository,
            EmailVerificationRepository emailVerificationRepository,
            UserChipRepository userChipRepository,
            AdminRoleService adminRoleService,
            ConsentRepository consentRepository,
            ConsentVerificationRepository consentVerificationRepository,
            AcceptanceTermsRepository acceptanceTermsRepository,
            PasswordRecoveryRepository passwordRecoveryRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.userSessionRepository = userSessionRepository;
        this.userConfigurationRepository = userConfigurationRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.userChipRepository = userChipRepository;
        this.adminRoleService = adminRoleService;
        this.consentRepository = consentRepository;
        this.consentVerificationRepository = consentVerificationRepository;
        this.acceptanceTermsRepository = acceptanceTermsRepository;
        this.passwordRecoveryRepository = passwordRecoveryRepository;
    }

    private UserDetailResponseDTO toDTO(User user) {
        String email = credentialRepository.findByUser(user)
                .map(Credential::getEmail)
                .orElse(null);

        String roleName = userRoleRepository.findByUserId(user.getIdUser())
                .map(ur -> ur.getRole().getNameRole().name())
                .orElse("Sin rol");

        boolean hasSession = userSessionRepository.existsByUser_IdUser(user.getIdUser());

        Optional<UserChip> activeChip = userChipRepository
                .findByUser_IdUserAndState(user.getIdUser(), UserChipStatus.ACTIVE);

        String chipName = activeChip.map(uc -> uc.getChip().getChipName()).orElse(null);
        String chipCode = activeChip.map(uc -> uc.getChip().getChipCode()).orElse(null);
        String programName = activeChip
                .map(uc -> uc.getChip().getProgram().getProgramName())
                .orElse(null);

        return new UserDetailResponseDTO(
                user.getIdUser(),
                user.getFirstName(),
                user.getLastName(),
                user.getDocumentNumber(),
                user.getDocumentType().getName(),
                user.getBirthDate(),
                email,
                roleName,
                user.getAccountStatus().name(),
                user.getCreatedAt(),
                chipName,
                chipCode,
                programName,
                hasSession);
    }

    @Override
    public List<UserDetailResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> userSessionRepository.existsByUser_IdUser(u.getIdUser()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDetailResponseDTO> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            throw new UserManagementException("No se encontraron usuarios con ese criterio");
        }

        List<UserDetailResponseDTO> results = userRepository.searchByFullNameOrEmail(query).stream()
                .filter(u -> userSessionRepository.existsByUser_IdUser(u.getIdUser()))
                .map(this::toDTO)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            throw new UserManagementException("No se encontraron usuarios con ese criterio");
        }

        return results;
    }

    @Override
    public UserDetailResponseDTO getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));
        return toDTO(user);
    }

    @Override
    @Transactional
    public UserDetailResponseDTO updateUser(UUID userId, UpdateUserRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setAccountStatus(dto.getAccountStatus());
        userRepository.save(user);

        AssignRoleRequestDTO roleDto = new AssignRoleRequestDTO();
        roleDto.setRole(dto.getRole());
        adminRoleService.assignRole(userId, roleDto);

        return toDTO(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserManagementException("Usuario no encontrado"));

        boolean hasActiveChip = userChipRepository
                .existsByUser_IdUserAndState(userId, UserChipStatus.ACTIVE);
        if (hasActiveChip) {
            throw new UserManagementException(
                    "No se puede eliminar porque está vinculado a una ficha activa. Desvincula primero al aprendiz.");
        }

        consentRepository.findByUser(user).ifPresent(consent -> {
            consentVerificationRepository.findByConsent(consent)
                    .ifPresent(consentVerificationRepository::delete);
            consentRepository.delete(consent);
        });

        acceptanceTermsRepository.findByUser(user)
                .ifPresent(acceptanceTermsRepository::delete);

        List<PasswordRecovery> recoveries = passwordRecoveryRepository.findAllByUser_IdUser(userId);
        passwordRecoveryRepository.deleteAll(recoveries);

        List<UserSession> sessions = userSessionRepository.findByUser_IdUser(userId);
        userSessionRepository.deleteAll(sessions);

        List<EmailVerification> verifications = emailVerificationRepository.findAllByUser_IdUser(userId);
        emailVerificationRepository.deleteAll(verifications);

        userConfigurationRepository.findByUser_IdUser(userId)
                .ifPresent(userConfigurationRepository::delete);

        userRoleRepository.findByUserId(userId)
                .ifPresent(userRoleRepository::delete);

        credentialRepository.findByUser(user)
                .ifPresent(credentialRepository::delete);

        userRepository.delete(user);
    }
}
```

## 10. Persistencia y modelo relacionado

### `auth/repository/security/UserRepository.java`

```java
package com.FaceLit.backend.auth.repository.security;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.FaceLit.backend.auth.model.security.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByDocumentNumber(String documentNumber);

    Optional<User> findByDocumentNumber(String documentNumber);

    @Query("""
                SELECT u FROM User u
                JOIN Credential c ON c.user = u
                WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<User> searchByFullNameOrEmail(@Param("query") String query);
}
```

### `auth/repository/roleandpermission/UserRoleRepository.java`

```java
package com.FaceLit.backend.auth.repository.roleandpermission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.idUser = :idUser")
    Optional<UserRole> findByUserId(@Param("idUser") UUID idUser);
}
```

### Campos principales de `auth/model/security/User.java`

La entidad representa `security.user_app` y contiene estos campos expuestos o usados por RF-10:

```java
private UUID idUser;
private String documentNumber;
private String firstName;
private String lastName;
private LocalDate birthDate;
private AccountStatus accountStatus;
private boolean emailVerified;
private Credential credential;
private DocumentType documentType;
private List<UserChip> userChips;
```

- `idUser` es UUID generado por la base de datos/JPA.
- `documentNumber` no se edita desde RF-10.
- `documentType` y `birthDate` son de solo lectura en este modulo.
- El correo vive en `Credential`, no directamente en `User`.
- La ficha se obtiene desde `UserChip` y solo se considera la que tenga estado `ACTIVE`.

## 11. Errores y estados HTTP

| Situacion | HTTP | Cuerpo |
|---|---:|---|
| Consulta correcta | `200` | Lista u objeto JSON |
| Edicion correcta | `200` | Usuario actualizado |
| Eliminacion correcta | `204` | Sin cuerpo |
| Validacion de body | `400` | Mapa `{campo: mensaje}` |
| Usuario no encontrado | `400` | `{"message":"Usuario no encontrado"}` |
| Busqueda vacia/sin resultados | `400` | `{"message":"No se encontraron usuarios con ese criterio"}` |
| Ficha activa al eliminar | `400` | `{"message":"No se puede eliminar porque está vinculado a una ficha activa. Desvincula primero al aprendiz."}` |
| Sin rol permitido o sin JWT | `401`/`403` | Respuesta de Spring Security, segun el caso |
| Error no controlado | `500` | `{"message":"Error interno del servidor"}` |

## 12. Flujo recomendado para Front

1. Guardar el JWT obtenido en login y enviarlo como `Bearer`.
2. Cargar el panel con `GET /api/admin/users`.
3. Para buscar, llamar `GET /api/admin/users/search?query=...`; no enviar `query` vacio.
4. Para abrir detalle, usar el `userId` del registro.
5. Para editar, enviar el body completo de `UpdateUserRequestDTO`.
6. Para eliminar, tratar `204` como exito y `400` como error funcional mostrando el campo `message`.
7. No mostrar ni solicitar cambio de `documentNumber`, `documentType`, `birthDate` o `email`, porque RF-10 no los actualiza.

## 13. Observacion de consistencia del codigo actual

Existe un `UserListResponseDTO` usado por el modulo historico de roles, pero `UserManagementController` responde actualmente `UserDetailResponseDTO` tanto para el listado como para la busqueda. Front debe tipar RF-10 con los campos de `UserDetailResponseDTO` descritos en este documento.

## 14. Codigo del endpoint historico de roles

### `auth/controller/roleandpermission/AdminRoleController.java`

```java
package com.FaceLit.backend.auth.controller.roleandpermission;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.FaceLit.backend.auth.dto.request.roleandpermission.AssignRoleRequestDTO;
import com.FaceLit.backend.auth.dto.response.roleandpermission.AssignRoleResponseDTO;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<AssignRoleResponseDTO> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequestDTO dto) {
        return ResponseEntity.ok(adminRoleService.assignRole(userId, dto));
    }
}
```

### `auth/dto/request/roleandpermission/AssignRoleRequestDTO.java`

```java
package com.FaceLit.backend.auth.dto.request.roleandpermission;

import jakarta.validation.constraints.NotNull;
import com.FaceLit.backend.auth.model.enums.RoleName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequestDTO {

    @NotNull(message = "El rol es obligatorio")
    private RoleName role;
}
```

## 15. Configuracion que protege el modulo

El bloque vigente de `config/SecurityConfig.java` es:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/api/consent/**").permitAll()
        .requestMatchers("/api/catalogos/**").permitAll()
        .requestMatchers("/api/admin/**")
        .hasAnyRole("ADMINISTRATOR", "COORDINATOR")
        .requestMatchers("/api/instructor/**")
        .hasAnyRole("ADMINISTRATOR", "COORDINATOR", "INSTRUCTOR")
        .requestMatchers("/api/profile/**").authenticated()
        .requestMatchers("/api/apprentice/**")
        .hasAnyRole("ADMINISTRATOR", "COORDINATOR", "INSTRUCTOR", "APPRENTICE")
        .anyRequest().authenticated())
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

El backend tambien tiene CORS habilitado para los metodos `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` y `PATCH`, y permite cualquier origen configurado por `allowedOriginPatterns("*")`.
