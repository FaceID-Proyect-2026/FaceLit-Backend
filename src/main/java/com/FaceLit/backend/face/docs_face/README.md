Endpoints y JSON de prueba — FaceLit Backend — Dispositivos
5. Módulo: Dispositivos
BASE URL

```
http://localhost:8080
```
5.1 Crear Dispositivo

Endpoint
```
POST /api/devices
```

JSON
```
{
  "deviceCode": "DEVICE-001",
  "location": "Entrada principal",
  "status": "ACTIVE",
  "originIp": "192.168.1.100"
}
```

Respuesta — 201 CREATED
```
{
  "idDevice": "UUID_DEL_DISPOSITIVO",
  "deviceCode": "DEVICE-001",
  "location": "Entrada principal",
  "status": "ACTIVE",
  "originIp": "192.168.1.100",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": "Dispositivo creado correctamente"
}
```
5.2 Actualizar Dispositivo

Endpoint
```
PUT /api/devices/{id}
```

JSON
```
{
  "deviceCode": "DEVICE-001",
  "location": "Entrada secundaria",
  "status": "INACTIVE",
  "originIp": "192.168.1.101"
}
```

Respuesta — 200 OK
```
{
  "idDevice": "UUID_DEL_DISPOSITIVO",
  "deviceCode": "DEVICE-001",
  "location": "Entrada secundaria",
  "status": "INACTIVE",
  "originIp": "192.168.1.101",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:45:00",
  "message": "Dispositivo actualizado correctamente"
}
```
5.3 Listar Dispositivos

Endpoint
```
GET /api/devices
```

Respuesta — 200 OK
```
[
  {
    "idDevice": "UUID_DEL_DISPOSITIVO",
    "deviceCode": "DEVICE-001",
    "location": "Entrada principal",
    "status": "ACTIVE",
    "originIp": "192.168.1.100",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```
5.4 Consultar por ID

Endpoint
```
GET /api/devices/{id}
```

Respuesta — 200 OK
```
{
  "idDevice": "UUID_DEL_DISPOSITIVO",
  "deviceCode": "DEVICE-001",
  "location": "Entrada principal",
  "status": "ACTIVE",
  "originIp": "192.168.1.100",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": null
}
```
5.5 Consultar por Código

Endpoint
```
GET /api/devices/code?deviceCode=DEVICE-001
```

Respuesta — 200 OK
```
{
  "idDevice": "UUID_DEL_DISPOSITIVO",
  "deviceCode": "DEVICE-001",
  "location": "Entrada principal",
  "status": "ACTIVE",
  "originIp": "192.168.1.100",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": null
}
```
5.6 Filtrar por Estado

Endpoint
```
GET /api/devices/status?status=ACTIVE
```

Respuesta — 200 OK
```
[
  {
    "idDevice": "UUID_DEL_DISPOSITIVO",
    "deviceCode": "DEVICE-001",
    "location": "Entrada principal",
    "status": "ACTIVE",
    "originIp": "192.168.1.100",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```
5.7 Eliminar Dispositivo

Endpoint
```
DELETE /api/devices/{id}
```

Respuesta

204 NO CONTENT


No devuelve JSON.

5.8 Validaciones

deviceCode:
```
El código del dispositivo es obligatorio
El código del dispositivo no puede superar los 50 caracteres
```

location:
```
La ubicación no puede superar los 100 caracteres
```

status:
```
El estado del dispositivo es obligatorio
```

originIp:
```
La IP de origen no puede superar los 50 caracteres
```

Ejemplo de error
```
{
  "message": "El código del dispositivo es obligatorio"
}
```
5.9 Flujo de Prueba
1. POST /api/devices
   → crear dispositivo

2. GET /api/devices
   → verificar listado

3. GET /api/devices/{id}
   → consultar dispositivo

4. GET /api/devices/code?deviceCode=DEVICE-001
   → consultar por código

5. GET /api/devices/status?status=ACTIVE
   → consultar por estado

6. PUT /api/devices/{id}
   → actualizar dispositivo

7. DELETE /api/devices/{id}
   → eliminar dispositivo
   → 204 NO CONTENT

# Endpoints y JSON de prueba — FaceLit Backend — User Faces

## 6. Módulo: User Faces

### BASE URL

```text
http://localhost:8080
```

### 6.1 Crear Cara Biométrica

**Endpoint**

```http
POST /api/user-faces
```

**JSON**

```json
{
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "biometricVector": [1, 2, 3, 4, 5],
  "registrationDate": "2026-08-21T08:30:00",
  "status": "ACTIVE"
}
```

**Respuesta — 201 CREATED**

```json
{
  "idUserFace": "UUID_DE_LA_CARA",
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "registrationDate": "2026-08-21T08:30:00",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": "Cara biométrica registrada correctamente"
}
```

### 6.2 Actualizar Cara Biométrica

**Endpoint**

```http
PUT /api/user-faces/{id}
```

**JSON**

```json
{
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "biometricVector": [1, 2, 3, 4, 5],
  "registrationDate": "2026-08-21T08:45:00",
  "status": "ACTIVE"
}
```

**Respuesta — 200 OK**

```json
{
  "idUserFace": "UUID_DE_LA_CARA",
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "registrationDate": "2026-08-21T08:45:00",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:45:00",
  "message": "Cara biométrica actualizada correctamente"
}
```

### 6.3 Listar Caras Biométricas

**Endpoint**

```http
GET /api/user-faces
```

**Respuesta — 200 OK**

```json
[
  {
    "idUserFace": "UUID_DE_LA_CARA",
    "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "registrationDate": "2026-08-21T08:30:00",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```

### 6.4 Consultar por ID

**Endpoint**

```http
GET /api/user-faces/{id}
```

**Respuesta — 200 OK**

```json
{
  "idUserFace": "UUID_DE_LA_CARA",
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "registrationDate": "2026-08-21T08:30:00",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": null
}
```

### 6.5 Consultar por Usuario

**Endpoint**

```http
GET /api/user-faces/user/{idUserApp}
```

**Respuesta — 200 OK**

```json
[
  {
    "idUserFace": "UUID_DE_LA_CARA",
    "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "registrationDate": "2026-08-21T08:30:00",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```

### 6.6 Filtrar por Estado

**Endpoint**

```http
GET /api/user-faces/status?status=ACTIVE
```

**Estados**

```text
ACTIVE
PENDING
INACTIVE
```

**Respuesta — 200 OK**

```json
[
  {
    "idUserFace": "UUID_DE_LA_CARA",
    "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "registrationDate": "2026-08-21T08:30:00",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```

### 6.7 Eliminar Cara Biométrica

**Endpoint**

```http
DELETE /api/user-faces/{id}
```

**Respuesta**

```text
204 NO CONTENT
```

No devuelve JSON.

> La eliminación cambia el estado a `INACTIVE`; no elimina físicamente el registro.

### 6.8 Validaciones

**idUserApp**

```text
El usuario es obligatorio
```

**biometricVector**

```text
El vector biométrico es obligatorio
```

**registrationDate**

```text
La fecha de registro es obligatoria
```

**status**

```text
El estado de la cara es obligatorio
```

**Ejemplo de error**

```json
{
  "message": "El usuario es obligatorio"
}
```

### 6.9 Flujo de Prueba

1. `POST /api/user-faces`
   → crear cara

2. `GET /api/user-faces`
   → listar caras

3. `GET /api/user-faces/{id}`
   → consultar por ID

4. `GET /api/user-faces/user/{idUserApp}`
   → consultar por usuario

5. `GET /api/user-faces/status?status=ACTIVE`
   → consultar por estado

6. `PUT /api/user-faces/{id}`
   → actualizar cara

7. `DELETE /api/user-faces/{id}`
   → cambiar a `INACTIVE`
   → `204 NO CONTENT`

# Endpoints y JSON de prueba — FaceLit Backend — Facial Events

## 7. Módulo: Facial Events

### BASE URL

```text
http://localhost:8080
```

### 7.1 Crear Evento Facial

**Endpoint**

```http
POST /api/admin/facial-events
```

**JSON**

```json
{
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "idEnvironment": "650e8400-e29b-41d4-a716-446655440000",
  "idChip": "750e8400-e29b-41d4-a716-446655440000",
  "idDevice": "850e8400-e29b-41d4-a716-446655440000",
  "eventDatetime": "2026-08-21T08:30:00",
  "eventType": "ENTRY",
  "recognitionResult": "RECOGNIZED",
  "sendStatus": "PENDING",
  "origin": "ONLINE"
}
```

**Respuesta — 201 CREATED**

```json
{
  "idFacialEvent": "UUID_DEL_EVENTO",
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "idEnvironment": "650e8400-e29b-41d4-a716-446655440000",
  "idChip": "750e8400-e29b-41d4-a716-446655440000",
  "idDevice": "850e8400-e29b-41d4-a716-446655440000",
  "eventDatetime": "2026-08-21T08:30:00",
  "eventType": "ENTRY",
  "recognitionResult": "RECOGNIZED",
  "sendStatus": "PENDING",
  "origin": "ONLINE",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": "Evento facial registrado correctamente"
}
```

### 7.2 Actualizar Evento Facial

**Endpoint**

```http
PUT /api/admin/facial-events/{id}
```

**JSON**

```json
{
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "idEnvironment": "650e8400-e29b-41d4-a716-446655440000",
  "idChip": "750e8400-e29b-41d4-a716-446655440000",
  "idDevice": "850e8400-e29b-41d4-a716-446655440000",
  "eventDatetime": "2026-08-21T08:45:00",
  "eventType": "EXIT",
  "recognitionResult": "RECOGNIZED",
  "sendStatus": "SENT",
  "origin": "ONLINE"
}
```

**Respuesta — 200 OK**

```json
{
  "idFacialEvent": "UUID_DEL_EVENTO",
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "idEnvironment": "650e8400-e29b-41d4-a716-446655440000",
  "idChip": "750e8400-e29b-41d4-a716-446655440000",
  "idDevice": "850e8400-e29b-41d4-a716-446655440000",
  "eventDatetime": "2026-08-21T08:45:00",
  "eventType": "EXIT",
  "recognitionResult": "RECOGNIZED",
  "sendStatus": "SENT",
  "origin": "ONLINE",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:45:00",
  "message": "Evento facial actualizado correctamente"
}
```

### 7.3 Listar Eventos Faciales

**Endpoint**

```http
GET /api/admin/facial-events
```

**Respuesta — 200 OK**

```json
[
  {
    "idFacialEvent": "UUID_DEL_EVENTO",
    "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
    "idEnvironment": "650e8400-e29b-41d4-a716-446655440000",
    "idChip": "750e8400-e29b-41d4-a716-446655440000",
    "idDevice": "850e8400-e29b-41d4-a716-446655440000",
    "eventDatetime": "2026-08-21T08:30:00",
    "eventType": "ENTRY",
    "recognitionResult": "RECOGNIZED",
    "sendStatus": "PENDING",
    "origin": "ONLINE",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```

### 7.4 Consultar por ID

**Endpoint**

```http
GET /api/admin/facial-events/{id}
```

**Respuesta — 200 OK**

```json
{
  "idFacialEvent": "UUID_DEL_EVENTO",
  "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
  "idEnvironment": "650e8400-e29b-41d4-a716-446655440000",
  "idChip": "750e8400-e29b-41d4-a716-446655440000",
  "idDevice": "850e8400-e29b-41d4-a716-446655440000",
  "eventDatetime": "2026-08-21T08:30:00",
  "eventType": "ENTRY",
  "recognitionResult": "RECOGNIZED",
  "sendStatus": "PENDING",
  "origin": "ONLINE",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": null
}
```

### 7.5 Consultar por Usuario

**Endpoint**

```http
GET /api/admin/facial-events/user/{idUserApp}
```

**Respuesta — 200 OK**

```json
[
  {
    "idFacialEvent": "UUID_DEL_EVENTO",
    "idUserApp": "550e8400-e29b-41d4-a716-446655440000",
    "idEnvironment": "650e8400-e29b-41d4-a716-446655440000",
    "idChip": "750e8400-e29b-41d4-a716-446655440000",
    "idDevice": "850e8400-e29b-41d4-a716-446655440000",
    "eventDatetime": "2026-08-21T08:30:00",
    "eventType": "ENTRY",
    "recognitionResult": "RECOGNIZED",
    "sendStatus": "PENDING",
    "origin": "ONLINE",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```

### 7.6 Consultar por Ambiente

**Endpoint**

```http
GET /api/admin/facial-events/environment/{idEnvironment}
```

### 7.7 Consultar por Dispositivo

**Endpoint**

```http
GET /api/admin/facial-events/device/{idDevice}
```

### 7.8 Filtrar por Tipo

**Endpoint**

```http
GET /api/admin/facial-events/type?eventType=ENTRY
```

**Tipos**

```text
ENTRY
BREAK
EXIT
```

### 7.9 Filtrar por Resultado

**Endpoint**

```http
GET /api/admin/facial-events/recognition-result?recognitionResult=RECOGNIZED
```

**Resultados**

```text
RECOGNIZED
UNRECOGNIZED
```

### 7.10 Filtrar por Estado de Envío

**Endpoint**

```http
GET /api/admin/facial-events/send-status?sendStatus=PENDING
```

**Estados**

```text
PENDING
SENT
```

### 7.11 Eliminar Evento Facial

**Endpoint**

```http
DELETE /api/admin/facial-events/{id}
```

**Respuesta**

```text
204 NO CONTENT
```

No devuelve JSON.

> En este caso el evento se elimina físicamente de la base de datos.

### 7.12 Validaciones

**idEnvironment**

```text
El ambiente es obligatorio
```

**idDevice**

```text
El dispositivo es obligatorio
```

**eventDatetime**

```text
La fecha del evento es obligatoria
```

**eventType**

```text
El tipo de evento es obligatorio
```

**recognitionResult**

```text
El resultado del reconocimiento es obligatorio
```

**sendStatus**

```text
El estado de envío es obligatorio
```

**origin**

```text
El origen del evento es obligatorio
```

### 7.13 Flujo de Prueba

1. `POST /api/admin/facial-events`
   → crear evento facial

2. `GET /api/admin/facial-events`
   → listar eventos

3. `GET /api/admin/facial-events/{id}`
   → consultar por ID

4. `GET /api/admin/facial-events/user/{idUserApp}`
   → consultar por usuario

5. `GET /api/admin/facial-events/environment/{idEnvironment}`
   → consultar por ambiente

6. `GET /api/admin/facial-events/device/{idDevice}`
   → consultar por dispositivo

7. `GET /api/admin/facial-events/type?eventType=ENTRY`
   → consultar por tipo

8. `GET /api/admin/facial-events/recognition-result?recognitionResult=RECOGNIZED`
   → consultar por resultado

9. `GET /api/admin/facial-events/send-status?sendStatus=PENDING`
   → consultar por estado de envío

10. `PUT /api/admin/facial-events/{id}`
    → actualizar evento

11. `DELETE /api/admin/facial-events/{id}`
    → eliminar evento
    → `204 NO CONTENT`

Claro, en el mismo formato simplificado:

# Endpoints y JSON de prueba — FaceLit Backend — Biometric Logs

## 8. Módulo: Biometric Logs

### BASE URL

```text
http://localhost:8080
```

### 8.1 Crear Log Biométrico

**Endpoint**

```http
POST /api/admin/biometric-logs
```

**JSON**

```json
{
  "idFacialEvent": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Reconocimiento facial realizado correctamente",
  "logDate": "2026-08-21T08:30:00"
}
```

**Respuesta — 201 CREATED**

```json
{
  "idBiometricLog": "UUID_DEL_LOG",
  "idFacialEvent": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Reconocimiento facial realizado correctamente",
  "logDate": "2026-08-21T08:30:00",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": "Log biométrico registrado correctamente"
}
```

### 8.2 Actualizar Log Biométrico

**Endpoint**

```http
PUT /api/admin/biometric-logs/{id}
```

**JSON**

```json
{
  "idFacialEvent": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Log biométrico actualizado",
  "logDate": "2026-08-21T08:45:00"
}
```

**Respuesta — 200 OK**

```json
{
  "idBiometricLog": "UUID_DEL_LOG",
  "idFacialEvent": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Log biométrico actualizado",
  "logDate": "2026-08-21T08:45:00",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:45:00",
  "message": "Log biométrico actualizado correctamente"
}
```

### 8.3 Listar Logs Biométricos

**Endpoint**

```http
GET /api/admin/biometric-logs
```

**Respuesta — 200 OK**

```json
[
  {
    "idBiometricLog": "UUID_DEL_LOG",
    "idFacialEvent": "550e8400-e29b-41d4-a716-446655440000",
    "description": "Reconocimiento facial realizado correctamente",
    "logDate": "2026-08-21T08:30:00",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```

### 8.4 Consultar por ID

**Endpoint**

```http
GET /api/admin/biometric-logs/{id}
```

**Respuesta — 200 OK**

```json
{
  "idBiometricLog": "UUID_DEL_LOG",
  "idFacialEvent": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Reconocimiento facial realizado correctamente",
  "logDate": "2026-08-21T08:30:00",
  "createdAt": "2026-08-21T08:30:00",
  "updatedAt": "2026-08-21T08:30:00",
  "message": null
}
```

### 8.5 Consultar por Evento Facial

**Endpoint**

```http
GET /api/admin/biometric-logs/facial-event/{idFacialEvent}
```

**Respuesta — 200 OK**

```json
[
  {
    "idBiometricLog": "UUID_DEL_LOG",
    "idFacialEvent": "550e8400-e29b-41d4-a716-446655440000",
    "description": "Reconocimiento facial realizado correctamente",
    "logDate": "2026-08-21T08:30:00",
    "createdAt": "2026-08-21T08:30:00",
    "updatedAt": "2026-08-21T08:30:00",
    "message": null
  }
]
```

### 8.6 Eliminar Log Biométrico

**Endpoint**

```http
DELETE /api/admin/biometric-logs/{id}
```

**Respuesta**

```text
204 NO CONTENT
```

No devuelve JSON.

> En este caso el log biométrico se elimina físicamente de la base de datos.

### 8.7 Validaciones

**idFacialEvent**

```text
El evento facial es obligatorio
```

**description**

```text
La descripción no puede superar los 2000 caracteres
```

**logDate**

```text
La fecha del log es obligatoria
```

**Ejemplo de error**

```json
{
  "message": "El evento facial es obligatorio"
}
```

### 8.8 Flujo de Prueba

1. `POST /api/admin/biometric-logs`
   → crear log biométrico

2. `GET /api/admin/biometric-logs`
   → listar logs

3. `GET /api/admin/biometric-logs/{id}`
   → consultar por ID

4. `GET /api/admin/biometric-logs/facial-event/{idFacialEvent}`
   → consultar logs de un evento facial

5. `PUT /api/admin/biometric-logs/{id}`
   → actualizar log

6. `DELETE /api/admin/biometric-logs/{id}`
   → eliminar log
   → `204 NO CONTENT`
