# 🔐 FaceLit Backend - Configuración con Spring Boot + PostgreSQL

![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-purple)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

Backend del proyecto **FaceLit**, construido con Spring Boot, conectado a PostgreSQL y con autenticación basada en JWT.
Este documento explica la configuración del entorno, variables sensibles y estructura base del proyecto.

---

## 📌 Tabla de Contenido

* [📁 Variables de Entorno](#-variables-de-entorno)
* [⚙️ Configuración Spring Boot](#️-configuración-spring-boot)
* [🗄️ Base de Datos](#️-base-de-datos)
* [🔐 Seguridad JWT](#-seguridad-jwt)
* [📦 Dependencias](#-dependencias)
* [🚀 Ejecución del Proyecto](#-ejecución-del-proyecto)
* [🛡️ Buenas Prácticas](#️-buenas-prácticas)

---

## 📁 Variables de Entorno

Se utiliza un archivo `.env` para manejar configuración sensible.

### 🔧 Ejemplo `.env`

```env
DB_URL=jdbc:postgresql://localhost:5439/facelit
DB_USERNAME=facelit_user
DB_PASSWORD=facelit_password
JWT_SECRET=clave-secreta-super-larga-de-al-menos-32-caracteres-facelit-2025
JWT_EXPIRATION=28800000
```

### 📌 Descripción

| Variable       | Descripción                      |
| -------------- | -------------------------------- |
| DB_URL         | URL de conexión a PostgreSQL     |
| DB_USERNAME    | Usuario de la base de datos      |
| DB_PASSWORD    | Contraseña                       |
| JWT_SECRET     | Clave secreta para firmar tokens |
| JWT_EXPIRATION | Tiempo de expiración (ms = 28800000, horas = 8)        |

---

## ⚙️ Configuración Spring Boot

📍 `src/main/resources/application.yml`

### 🔌 Importar `.env`

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```
- Permite cargar el archivo .env
- optional: evita errores si no existe

---

## 🗄️ Base de Datos

### 🔗 Conexión

```yaml
datasource:
  url: ${DB_URL}
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}
  driver-class-name: org.postgresql.Driver
```
- Usa variables del .env
- Define la conexión con PostgreSQL

### ⚡ Pool de conexiones (Hikari)

```yaml
hikari:
  connection-timeout: 20000
  maximum-pool-size: 5
```
- connection-timeout → Tiempo máximo de espera (20 segundos)
- maximum-pool-size → Máximo de conexiones simultáneas

>  Mejora el rendimiento y manejo de conexiones

---

## 🧠 JPA / Hibernate

```yaml
jpa:
  hibernate:
    ddl-auto: validate
  show-sql: true
```
- ddl-auto: validate → Verifica que las tablas existan (no las crea)
- show-sql: true → Muestra las consultas SQL en consola

### ⚙️ Propiedades adicionales

```yaml
properties:
  hibernate:
    format_sql: true
    dialect: org.hibernate.dialect.PostgreSQLDialect
```
- format_sql → Hace el SQL más legible
- dialect → Indica el tipo de base de datos (PostgreSQL)

---

### 🚫 Configuración de Liquibase

```yaml
liquibase:
  enabled: false
```
- Desactiva migraciones automáticas
- Las tablas deben existir previamente

---

### 🌐 Configuración del servidor

```yaml
server:
  port: 8080
```
- Define el puerto donde corre el backend
- URL de acceso: http://localhost:8080
---

## 🔐 Seguridad JWT

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION}
```

✔ Control de autenticación basado en tokens
✔ Manejo de sesiones sin estado (stateless)
- secret → Clave para firmar tokens
- expiration → Tiempo de validez del token

---

## 📦 Dependencias

### 📌 `spring-dotenv`

Permite que Spring Boot lea variables del `.env`.

```xml
<dependency>
  <groupId>me.paulschwarz</groupId>
  <artifactId>spring-dotenv</artifactId>
  <version>4.0.0</version>
</dependency>
```
---

## 🚀 Ejecución del Proyecto

### 1️⃣ Clonar repositorio

```bash
git clone https://github.com/tu-usuario/facelit-backend.git
cd facelit-backend
```

### 2️⃣ Crear archivo `.env`

```bash
cp .env.example .env
```

### 3️⃣ Ejecutar

```bash
./mvnw spring-boot:run
```

o

```bash
mvn spring-boot:run
```

---

## 🌐 Servidor

```yaml
server:
  port: 8080
```

🔗 Acceso:
👉 http://localhost:8080

---

## 🚫 `.gitignore`

```gitignore
.env
*.env
!.env.example
```

✔ Evita subir credenciales
✔ Permite compartir configuración base

---

## 🛡️ Buenas Prácticas

* 🔒 Nunca subir `.env` al repositorio
* 🔑 Usar claves JWT seguras (mínimo 32 caracteres)
* 🧪 Separar entornos (`dev`, `test`, `prod`)
* 📊 Limitar conexiones en producción (Hikari)
* 🧱 Usar migraciones (Liquibase) en proyectos grandes
* 📉 Desactivar `show-sql` en producción

---

## 📌 Notas Finales

* Este proyecto usa autenticación basada en JWT
* Las tablas deben existir previamente (`ddl-auto: validate`)
* Liquibase está deshabilitado

---

💡 *Tip:* Puedes integrar Docker + PostgreSQL para facilitar despliegues futuros.

---

## 👩‍💻 Autora

**María José Rodríguez**
Proyecto académico - ADSO 🚀

---
