# Módulo Face

## Descripción general

El módulo `face` centraliza toda la lógica relacionada con reconocimiento facial, gestión de biometría, eventos de acceso y dispositivos de captura dentro del backend de FaceLit.

Su objetivo es manejar de forma organizada:

- registro y validación de rostros de usuarios
- control de dispositivos de captura
- eventos de reconocimiento facial
- trazabilidad de logs biométricos
- comunicación con otras entidades del sistema como usuarios, ambientes y chips

Este módulo sigue la misma estructura de capas usada en el proyecto:

- model
- repository
- dto
- service
- exception
- controller

---

## Estructura del módulo

```text
face/
├── controller/
│   └── facialrecognition/
│       ├── DeviceController.java
│       ├── UserFaceController.java
│       ├── FacialEventController.java
│       └── BiometricLogController.java
├── dto/
│   ├── request/
│   │   └── facialrecognition/
│   │       ├── DeviceRequestDTO.java
│   │       ├── UserFaceRequestDTO.java
│   │       ├── FacialEventRequestDTO.java
│   │       └── BiometricLogRequestDTO.java
│   ├── response/
│   │   └── facialrecognition/
│   │       ├── DeviceResponseDTO.java
│   │       ├── UserFaceResponseDTO.java
│   │       ├── FacialEventResponseDTO.java
│   │       └── BiometricLogResponseDTO.java
│   └── validation/
│       └── ValidBiometricVector.java
├── exception/
│   ├── DeviceNotFoundException.java
│   ├── UserFaceNotFoundException.java
│   ├── FacialEventNotFoundException.java
│   └── BiometricLogNotFoundException.java
├── model/
│   ├── enums/
│   │   ├── DeviceStatus.java
│   │   ├── FaceStatus.java
│   │   ├── EventType.java
│   │   ├── RecognitionResult.java
│   │   ├── SendStatus.java
│   │   └── EventOrigin.java
│   └── facialrecognition/
│       ├── Device.java
│       ├── UserFace.java
│       ├── FacialEvent.java
│       └── BiometricLog.java
├── repository/
│   └── facialrecognition/
│       ├── DeviceRepository.java
│       ├── UserFaceRepository.java
│       ├── FacialEventRepository.java
│       └── BiometricLogRepository.java
├── service/
│   ├── facialrecognition/
│   │   ├── DeviceService.java
│   │   ├── UserFaceService.java
│   │   ├── FacialEventService.java
│   │   └── BiometricLogService.java
│   └── facialrecognition/impl/
│       ├── DeviceServiceImpl.java
│       ├── UserFaceServiceImpl.java
│       ├── FacialEventServiceImpl.java
│       └── BiometricLogServiceImpl.java
└── README.md
```

---

## Entidades del módulo

### 1. Device

Representa un dispositivo físico o lógico que participa en el proceso de reconocimiento facial.

Campos principales:

- idDevice
- deviceCode
- location
- status
- originIp

Relaciones:

- un dispositivo puede generar varios eventos de reconocimiento
- mapea la tabla `facialrecognition.device`

Estado:

- enum `DeviceStatus`
- valores típicos: `ACTIVE`, `INACTIVE`, `MAINTENANCE`

---

### 2. UserFace

Representa la biometría asociada a un usuario de la aplicación.

Campos principales:

- idUserFace
- user
- biometricVector
- registrationDate
- status

Relaciones:

- muchos rostros pertenecen a un usuario
- cada usuario puede tener varios registros faciales
- se relaciona con `User` mediante `id_user_app`

Estado:

- enum `FaceStatus`
- valores típicos: `PENDING`, `ACTIVE`, `REJECTED`, `INACTIVE`

El campo `biometricVector` se almacena como arreglo de bytes y valida que no venga vacío.

---

### 3. FacialEvent

Es la entidad central del flujo de reconocimiento.

Campos principales:

- idFacialEvent
- user
- environment
- chip
- device
- eventDatetime
- eventType
- recognitionResult
- sendStatus
- origin

Relaciones:

- un evento pertenece a un usuario
- un evento ocurre en un ambiente
- puede involucrar un chip
- se registra desde un dispositivo
- tiene muchos logs biométricos asociados

Enums relevantes:

- `EventType`: tipo del evento
- `RecognitionResult`: resultado del reconocimiento
- `SendStatus`: estado del envío del evento
- `EventOrigin`: origen del evento

---

### 4. BiometricLog

Representa el historial de eventos asociados a una lectura biométrica o reconocimiento.

Campos principales:

- idBiometricLog
- facialEvent
- description
- logDate

Relaciones:

- muchos logs corresponden a un facialEvent
- usado para auditoría y trazabilidad del proceso

---

## Relación entre entidades

La estructura del módulo está diseñada para reflejar los flujos reales del reconocimiento facial:

- `User` puede tener varios `UserFace`
- `UserFace` representa la huella biométrica del usuario
- `Device` genera eventos de reconocimiento
- `Environment` recibe los eventos en un punto físico
- `Chip` puede estar ligado al evento cuando la captura usa hardware asociado
- `FacialEvent` centraliza la operación
- `BiometricLog` registra la trazabilidad del evento

### Diagrama conceptual

```text
User 1 --- * UserFace
User 1 --- * FacialEvent
Environment 1 --- * FacialEvent
Device 1 --- * FacialEvent
Chip 1 --- * FacialEvent
FacialEvent 1 --- * BiometricLog
```

---

## Enums del módulo

Los enums están definidos bajo el paquete `face.model.enums`:

- `DeviceStatus`
- `FaceStatus`
- `EventType`
- `RecognitionResult`
- `SendStatus`
- `EventOrigin`

Estos valores permiten guardar estados y resultados de forma consistente en base de datos, usando `EnumType.STRING` para facilitar lectura y mantenimiento.

---

## Repositorios

Los repositorios del módulo se ubican en `face.repository.facialrecognition`:

- `DeviceRepository`
- `UserFaceRepository`
- `FacialEventRepository`
- `BiometricLogRepository`

Su responsabilidad es abstraer la persistencia y permitir consultas por:

- usuario
- ambiente
- dispositivo
- evento
- estado
- tipo de reconocimiento

---

## DTOs

### Request DTOs

Se usan para recibir datos desde el cliente y validarlos antes de persistir.

- `DeviceRequestDTO`
- `UserFaceRequestDTO`
- `FacialEventRequestDTO`
- `BiometricLogRequestDTO`

### Response DTOs

Se usan para devolver información estructurada al cliente sin exponer directamente entidades JPA.

- `DeviceResponseDTO`
- `UserFaceResponseDTO`
- `FacialEventResponseDTO`
- `BiometricLogResponseDTO`

### Validación

Se implementaron validaciones específicas, por ejemplo:

- `ValidBiometricVector`

Esto ayuda a asegurar que el vector biométrico no sea nulo ni vacío antes de guardar un registro facial.

---

## Servicios

La capa de servicio está dividida entre interfaz y lógica de implementación.

### Interfaces

- `DeviceService`
- `UserFaceService`
- `FacialEventService`
- `BiometricLogService`

### Implementaciones

- `DeviceServiceImpl`
- `UserFaceServiceImpl`
- `FacialEventServiceImpl`
- `BiometricLogServiceImpl`

### Responsabilidades

- crear registros
- actualizar información
- consultar por ID o por relaciones
- eliminar registros
- aplicar reglas de negocio
- mapear entidades a DTOs

---

## Excepciones

El módulo incluye excepciones personalizadas para manejar errores específicos:

- `DeviceNotFoundException`
- `UserFaceNotFoundException`
- `FacialEventNotFoundException`
- `BiometricLogNotFoundException`

Esto mantiene la API más clara y facilita el manejo de errores por dominio.

---

## Controladores REST

El módulo expone endpoints bajo rutas administrativas del tipo `/api/admin/...`.

### DeviceController

Base: `/api/admin/devices`

- `POST /api/admin/devices` → Crear dispositivo
- `PUT /api/admin/devices/{id}` → Actualizar dispositivo
- `GET /api/admin/devices` → Listar dispositivos
- `GET /api/admin/devices/{id}` → Obtener dispositivo por ID
- `GET /api/admin/devices/code?deviceCode=...` → Buscar por código
- `GET /api/admin/devices/status?status=...` → Filtrar por estado
- `DELETE /api/admin/devices/{id}` → Eliminar dispositivo

### UserFaceController

Base: `/api/admin/user-faces`

- `POST /api/admin/user-faces` → Crear registro facial
- `PUT /api/admin/user-faces/{id}` → Actualizar registro facial
- `GET /api/admin/user-faces` → Listar registros faciales
- `GET /api/admin/user-faces/{id}` → Obtener por ID
- `GET /api/admin/user-faces/user/{idUserApp}` → Obtener rostros por usuario
- `GET /api/admin/user-faces/status?status=...` → Filtrar por estado
- `DELETE /api/admin/user-faces/{id}` → Eliminar registro facial

### FacialEventController

Base: `/api/admin/facial-events`

- `POST /api/admin/facial-events` → Crear evento facial
- `PUT /api/admin/facial-events/{id}` → Actualizar evento facial
- `GET /api/admin/facial-events` → Listar eventos
- `GET /api/admin/facial-events/{id}` → Obtener evento por ID
- `GET /api/admin/facial-events/user/{idUserApp}` → Eventos por usuario
- `GET /api/admin/facial-events/environment/{idEnvironment}` → Eventos por ambiente
- `GET /api/admin/facial-events/device/{idDevice}` → Eventos por dispositivo
- `GET /api/admin/facial-events/type?eventType=...` → Eventos por tipo
- `GET /api/admin/facial-events/recognition-result?recognitionResult=...` → Eventos por resultado
- `GET /api/admin/facial-events/send-status?sendStatus=...` → Eventos por estado de envío
- `DELETE /api/admin/facial-events/{id}` → Eliminar evento

### BiometricLogController

Base: `/api/admin/biometric-logs`

- `POST /api/admin/biometric-logs` → Crear log biométrico
- `PUT /api/admin/biometric-logs/{id}` → Actualizar log
- `GET /api/admin/biometric-logs` → Listar logs
- `GET /api/admin/biometric-logs/{id}` → Obtener log por ID
- `GET /api/admin/biometric-logs/facial-event/{idFacialEvent}` → Logs por evento facial
- `DELETE /api/admin/biometric-logs/{id}` → Eliminar log

---

## Flujo típico del módulo

1. Un usuario se registra o ya existe en el sistema.
2. Se registra su vector biométrico mediante `UserFace`.
3. Un `Device` captura la lectura de reconocimiento.
4. Se genera un `FacialEvent` con el resultado del reconocimiento.
5. Se añaden `BiometricLog` para auditar el proceso.
6. El sistema responde con DTOs limpios y bien estructurados.

---

## Convención de nombres y arquitectura

El módulo sigue el patrón del resto del backend:

- entidades bajo `model`
- repositorios bajo `repository`
- request/response bajo `dto`
- lógica bajo `service` y `serviceImpl`
- validaciones bajo `validation`
- controladores bajo `controller`
- errores bajo `exception`

Esto permite mantener una organización consistente con el resto de módulos del proyecto, como `auth`, `academic`, `environments` y `schedule`.

---

## Observaciones de diseño

- Las entidades heredan de `AuditBase`, lo cual permite registrar auditoría del sistema.
- Se usa `@ManyToOne` y `@OneToMany` para modelar relaciones entre entidades.
- Las enumeraciones se guardan como texto (`EnumType.STRING`) para facilitar lectura en base de datos.
- Los DTOs evitan exponer entidades JPA directamente.
- La capa de servicio centraliza la lógica de negocio y validaciones.

---

## Resumen

El módulo Face es el núcleo del sistema de reconocimiento biométrico de FaceLit. Su responsabilidad principal es gestionar:

- usuarios con huellas faciales
- dispositivos de captura
- eventos de reconocimiento
- trazabilidad de cada interacción
- validación y persistencia segura de datos biométricos

Es un módulo crítico para la lógica de acceso, seguridad y monitoreo del sistema.
