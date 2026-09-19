# Patrones de diseño y calidad de código

## 1. Propósito

Este documento identifica los patrones que ya se aplican en FaceLit, explica cómo funcionan y relaciona cada patrón con sus archivos de implementación.

No se implementan patrones únicamente por cumplir una lista. `Strategy`, `Observer`, `Command`, `State`, `Decorator`, `Composite` y `Abstract Factory` quedan como opciones futuras porque el dominio actual no necesita todavía esas estructuras.

## 2. Patrones creacionales

### 2.1 Singleton gestionado por Spring

Spring crea una única instancia por defecto de los componentes registrados en el contenedor. El proyecto no implementa Singleton manualmente con constructores privados o campos estáticos.

Se aplica mediante:

- `@Service`: servicios de negocio.
- `@Repository`: repositorios JPA.
- `@Component`: componentes reutilizables.
- `@Configuration`: configuración de Spring.

Archivos representativos:

- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/ProgramServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/ChipServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/CsvAcademicServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/repository/ProgramRepository.java`
- `src/main/java/com/FaceLit/backend/config/JwtFilter.java`
- `src/main/java/com/FaceLit/backend/config/SecurityConfig.java`

Los servicios son stateless: no guardan datos de una petición en atributos mutables.

### 2.2 Factory Method

Un Factory Method concentra la creación de una respuesta para que el código cliente no tenga que conocer todos sus valores internos.

Implementación:

- `src/main/java/com/FaceLit/backend/auth/dto/response/roleandpermission/LoginResponseDTO.java`

Método:

```java
LoginResponseDTO.success(token, role, permissions, userId)
```

Este método crea una respuesta consistente para un inicio de sesión exitoso.

Otros métodos de fábrica existentes:

- `src/main/java/com/FaceLit/backend/auth/dto/response/roleandpermission/AssignRoleResponseDTO.java`
- `src/main/java/com/FaceLit/backend/auth/dto/response/security/UserConfigurationResponseDTO.java`

### 2.3 Builder

El proyecto utiliza builders proporcionados por librerías, principalmente:

- `Jwts.builder()` en `JwtServiceImpl` para construir tokens JWT.
- Lombok en DTOs y entidades para reducir boilerplate de getters, setters y constructores.

Archivo principal:

- `src/main/java/com/FaceLit/backend/auth/service/serviceImpl/roleandpermission/JwtServiceImpl.java`

No se agregó un Builder manual para DTOs sencillos. Se reserva para objetos con muchos campos opcionales o reglas de construcción complejas.

### 2.4 Abstract Factory

No aplica actualmente. El proyecto no tiene familias intercambiables de objetos relacionados, como proveedores completos de correo, SMS y notificaciones push.

## 3. Patrones estructurales

### 3.1 Facade

El controller expone la API y delega el flujo de negocio a un service. El controller no coordina repositorios, validaciones, transacciones ni bitácoras.

Implementaciones:

- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/ProgramServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/ChipServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/InstructorServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/UserChipServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/CsvAcademicServiceImpl.java`

Controllers que usan estas fachadas:

- `src/main/java/com/FaceLit/backend/academic/controller/academic/ProgramController.java`
- `src/main/java/com/FaceLit/backend/academic/controller/academic/ChipController.java`
- `src/main/java/com/FaceLit/backend/academic/controller/academic/InstructorController.java`
- `src/main/java/com/FaceLit/backend/academic/controller/academic/UserChipController.java`
- `src/main/java/com/FaceLit/backend/academic/controller/academic/CsvAcademicController.java`

`CsvAcademicServiceImpl` funciona como fachada y orquestador de la carga por fases. Es un punto que debe vigilarse porque concentra más responsabilidades que los demás services.

### 3.2 Adapter

Los DTOs adaptan entidades JPA al contrato público de la API. Así se evita exponer directamente entidades, relaciones lazy o detalles internos de persistencia.

DTOs académicos:

- `src/main/java/com/FaceLit/backend/academic/dto/response/academic/ProgramResponseDTO.java`
- `src/main/java/com/FaceLit/backend/academic/dto/response/academic/ChipResponseDTO.java`
- `src/main/java/com/FaceLit/backend/academic/dto/response/academic/InstructorResponseDTO.java`
- `src/main/java/com/FaceLit/backend/academic/dto/response/academic/UserChipResponseDTO.java`
- `src/main/java/com/FaceLit/backend/academic/dto/response/academic/PendingTransferResponseDTO.java`
- `src/main/java/com/FaceLit/backend/academic/dto/response/academic/CsvUploadResponseDTO.java`

DTOs de autenticación y usuarios siguen el mismo principio en sus respectivos paquetes `dto/request` y `dto/response`.

### 3.3 Proxy / interceptor de seguridad

`JwtFilter` intercepta cada request antes del controller. Lee el header `Authorization`, valida el JWT y coloca la autenticación en `SecurityContextHolder`.

Implementación:

- `src/main/java/com/FaceLit/backend/config/JwtFilter.java`

La cadena de seguridad que consume esa autenticación está en:

- `src/main/java/com/FaceLit/backend/config/SecurityConfig.java`

No es un Proxy GoF puro, pero cumple la función estructural de interceptar y controlar el acceso antes de la lógica principal.

### 3.4 Decorator

No se fuerza actualmente. No hay una familia de servicios donde agregar capacidades opcionales como reintentos, logging o rate limiting sin modificar el servicio base.

### 3.5 Composite

No aplica. El dominio actual no representa árboles jerárquicos recursivos.

## 4. Patrones de comportamiento

### 4.1 Base común de auditoría

`AuditBase` centraliza los campos de auditoría que comparten las entidades. JPA copia esos campos en las tablas mediante `@MappedSuperclass`.

Implementación:

- `src/main/java/com/FaceLit/backend/shared/model/AuditBase.java`

Entidades que aprovechan la base, entre otras:

- `src/main/java/com/FaceLit/backend/academic/model/academic/Program.java`
- `src/main/java/com/FaceLit/backend/academic/model/academic/Chip.java`
- `src/main/java/com/FaceLit/backend/academic/model/academic/Instructor.java`
- `src/main/java/com/FaceLit/backend/auth/model/security/User.java`
- `src/main/java/com/FaceLit/backend/auth/model/security/Credential.java`

Esto es una plantilla de estructura común basada en herencia JPA. No es Template Method GoF estricto porque no define un algoritmo con pasos y hooks sobrescribibles.

### 4.2 Strategy

No se formaliza todavía. Las validaciones actuales tienen pocos casos y un `if` claro es más mantenible que una jerarquía de estrategias. Puede evaluarse cuando las excepciones de horario o las validaciones CSV tengan muchos tipos independientes.

Archivo relacionado que debe vigilarse:

- `src/main/java/com/FaceLit/backend/schedule/service/serviceImpl/ScheduleExceptionServiceImpl.java`

### 4.3 Observer

No está implementado todavía. Es el candidato natural para notificaciones futuras mediante `ApplicationEventPublisher` y `@EventListener`, por ejemplo después de crear un horario, aprobar consentimiento o asignar un rol.

No se agregaron listeners porque todavía no existe un flujo de eventos consolidado que lo justifique.

### 4.4 Command

No está implementado como jerarquía de comandos. La auditoría actual se registra desde los servicios mediante `ChangeHistoryRepository`.

Archivos de la bitácora académica:

- `src/main/java/com/FaceLit/backend/academic/model/academic/ChangeHistory.java`
- `src/main/java/com/FaceLit/backend/academic/repository/ChangeHistoryRepository.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/ProgramServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/ChipServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/InstructorServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/UserChipServiceImpl.java`
- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/CsvAcademicServiceImpl.java`

Una futura solución con AOP podría envolver métodos anotados, pero no se añade mientras la auditoría directa siga siendo clara.

### 4.5 Chain of Responsibility

No se fuerza en este momento. Las validaciones del CSV están agrupadas por fase y las reglas son legibles. Puede introducirse si cada fila llega a tener una cadena grande de validadores independientes.

### 4.6 State

Los estados del dominio son enums y sus transiciones están controladas por los services:

- `src/main/java/com/FaceLit/backend/academic/model/enums/AcademicState.java`
- `src/main/java/com/FaceLit/backend/auth/model/enums/AccountStatus.java`
- `src/main/java/com/FaceLit/backend/auth/model/enums/CredentialStatus.java`
- `src/main/java/com/FaceLit/backend/academic/model/enums/PendingTransferStatus.java`

No se implementan clases State separadas porque las transiciones actuales todavía no tienen suficiente complejidad.

## 5. Antipatrones revisados

### God Class

El principal punto de atención es:

- `src/main/java/com/FaceLit/backend/academic/service/serviceImpl/academic/CsvAcademicServiceImpl.java`

Actualmente coordina lectura CSV, validación, creación de cuentas, programas, fichas, instructores, aprendices y traslados. Mantenerlo como fachada es válido, pero si crece debe dividirse en parser, validadores y procesadores por tipo de fila.

### Magic Numbers y Magic Strings

Se centralizaron reglas compartidas en:

- `src/main/java/com/FaceLit/backend/shared/constants/AppConstants.java`

Incluye límites CSV, longitudes, claims JWT, prefijo Bearer y expiración de recuperación de contraseña.

### Dependencias muertas

Se eliminaron imports y dependencias evidentes del flujo modificado. Las nuevas dependencias del CSV participan en procesamiento, persistencia o generación de credenciales.

### Swallowing exceptions

El JWT captura excepciones específicas:

- `src/main/java/com/FaceLit/backend/auth/service/serviceImpl/roleandpermission/JwtServiceImpl.java`

El handler global conserva un mensaje estable para el cliente, pero registra el error real:

- `src/main/java/com/FaceLit/backend/shared/exception/GlobalExceptionHandler.java`

### Primitive Obsession

Los estados principales utilizan enums en vez de strings:

- `AcademicState`
- `InstructorType`
- `PendingTransferStatus`
- `AccountStatus`
- `CredentialStatus`

Los campos de auditoría e historial siguen usando texto porque deben conservar valores variables y legibles para consulta.

### Endpoint duplicado

Las rutas académicas están separadas por controller y no se encontraron mappings duplicados exactos en los controllers actuales.

### Sobre-ingeniería

No se implementaron patrones futuros solo para cumplir una lista. La ausencia de Strategy, Observer, Command, State, Decorator, Composite y Abstract Factory es intencional y está justificada por el tamaño actual del dominio.

## 6. Regla de mantenimiento

Antes de agregar un nuevo patrón, comprobar:

1. Qué problema concreto resuelve.
2. Qué código duplicado o acoplado elimina.
3. Si mejora las pruebas y el mantenimiento.
4. Si encaja con la arquitectura Spring existente.

Un patrón debe simplificar el sistema, no agregar clases únicamente para que el proyecto parezca más complejo.
