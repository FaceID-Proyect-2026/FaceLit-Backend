<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?color=00C853&size=50&center=true&vCenter=true&width=900&font=Audiowide&lines=FACELIT+BACKEND;Sistema+de+Reconocimiento+Facial;Spring+Boot+%2B+PostgreSQL+%2B+JWT" />
</p>

<p align="center">
  <img src="./img/f9ac71baaa8d0d294289e8491da0a3f7.gif" width="350"/>
</p>

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring](https://img.shields.io/badge/SpringBoot-4.0.6-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-red)



# 🧩 Arquitectura del Proyecto

El backend de **FaceLit** está construido bajo una arquitectura:

> 🧠 **Arquitectura en N Capas con Spring Boot**

### 🎯 ¿Por qué esta arquitectura?

* Permite una separación clara de responsabilidades
* Facilita el mantenimiento y la escalabilidad del sistema
* Mejora la organización del código por capas (presentación, negocio, persistencia)
* Ideal para proyectos académicos con buenas prácticas profesionales
* Reduce el acoplamiento entre componentes

---

# 📦 Estructura Global del Proyecto

```bash
com.tuproyecto.sena/
├── config/           → Seguridad, JWT, Swagger
├── auth/             → RF-1: login, registro, roles
├── ambientes/        → RF-2: CRUD ambientes
├── academico/        → RF-3: programas, fichas
├── horarios/         → RF-4: horarios
├── facial/           → RF-5: integración Python
├── asistencias/      → RF-6: control de asistencia
├── reportes/         → RF-7: Excel, estadísticas
├── notificaciones/   → RF-8: email, push
├── perfil/           → RF-9: usuario
├── admin/            → RF-10/11: auditoría
└── shared/           → utilidades comunes
```

📌 Cada módulo sigue esta estructura interna basada en capas:

```bash
controller/   → Capa de presentación
service/      → Capa de lógica de negocio
repository/   → Capa de acceso a datos
model/        → Capa de persistencia (entidades)
dto/          → Capa de transferencia de datos
exception/    → Manejo de errores
```

---

# 🏗️ Estructura Base (Ejemplo: módulo auth)

```bash
auth/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
└── exception/
```

---

# 🧠 Explicación de la Arquitectura por Capas

## 🎮 Controller (Capa de Presentación)

Punto de entrada de la API.

* Recibe peticiones HTTP
* Valida datos de entrada
* Llama al Service

```java
@PostMapping("/registro")
```

---

## ⚙️ Service (Capa de Negocio)

Contiene la lógica de negocio.

* Aplica reglas del sistema
* Coordina entidades
* Orquesta procesos

---

## 🔧 ServiceImpl (Implementación de la lógica)

Implementación del Service.

* Separa contrato de implementación
* Facilita pruebas unitarias
* Mejora la mantenibilidad del código

---

## 🗄️ Repository (Capa de Persistencia)

Acceso a la base de datos.

* Extiende JpaRepository
* Genera consultas automáticamente

```java
interface UsuarioRepository extends JpaRepository<Usuario, Long>
```

---

## 📦 Model (Entity)

Representa tablas en la base de datos.

```java
@Entity
@Table(name = "usuarios")
```

---

## 🔄 DTO (Data Transfer Object)

### 📥 Request DTO

Datos que envía el cliente

### 📤 Response DTO

Datos que devuelve el backend

### ⚠️ ¿Por qué usar DTO?

* Evita exponer entidades directamente
* Mejora la seguridad
* Permite controlar la información enviada y recibida

---


# 🔥 Análisis de Spring Initializr

# Documentación de dependencias — Backend FaceLit
**Proyecto:** Sistema de reconocimiento facial SENA  
**Archivo:** `pom.xml`  
**Spring Boot:** 4.0.6  
**Java:** 17  
**Build tool:** Maven  

---

## Configuración base en Spring Initializr

| Campo | Valor | Razón |
|---|---|---|
| Project | Maven | Más documentado que Gradle, estándar académico |
| Language | Java | — |
| Spring Boot | 4.0.6 | Versión estable LTS. |
| Group | com.FaceLit | — |
| Artifact | backend | — |
| Package name | com.FaceLit.backend | — |
| Packaging | Jar | Estándar para aplicaciones Spring Boot |
| Java | 17 | LTS con compatibilidad total de todas las librerías del proyecto |

---

## Dependencias — análisis y estado

### ✅ Spring Web
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
**Para qué sirve:** Permite crear endpoints REST (`@RestController`, `@GetMapping`, `@PostMapping`, etc.) y levanta el servidor Tomcat embebido.  
**Estado:** Obligatoria. Sin esta no existen controllers ni endpoints HTTP.

---

### ✅ Spring Data JPA
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
**Para qué sirve:** Integra Hibernate con Spring. Permite mapear clases Java a tablas de PostgreSQL con `@Entity`, `@Table`, `@Column` y genera SQL automáticamente desde los Repository.  
**Estado:** Obligatoria. Es el puente entre los `model/` y la base de datos.

---

### ✅ Spring Security
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
**Para qué sirve:** Gestiona autenticación, autorización y protección de endpoints. Provee `BCryptPasswordEncoder` para hashear contraseñas y el filtro para validar JWT en cada request.  
**Estado:** Obligatoria.  
**⚠️ Atención:** En cuanto se agrega esta dependencia, Spring bloquea TODOS los endpoints automáticamente. Se debe crear `SecurityConfig.java` desde el primer día para permitir acceso público a `/auth/registrar` y `/auth/login`. Si no, el endpoint de registro va a devolver 403 siempre.

---

### ✅ Validation
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
**Para qué sirve:** Activa las anotaciones de validación en los DTOs: `@NotBlank`, `@Email`, `@Pattern`, `@Past`, `@NotNull`, `@Size`.  
**Estado:** Obligatoria. Sin esta dependencia las anotaciones existen en el código pero no hacen nada en tiempo de ejecución.

---

### ✅ PostgreSQL Driver
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
**Para qué sirve:** Conector JDBC que permite a Java comunicarse con PostgreSQL.  
**Estado:** Obligatoria. Sin el driver Spring Data JPA no sabe cómo conectarse a la base de datos.

---

### ✅ Liquibase Migration
```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```
**Para qué sirve:** Controla las versiones de la base de datos mediante changesets. Cada modificación en la BD se registra en archivos XML o SQL que Liquibase ejecuta en orden automáticamente al iniciar la app.  
**Estado:** Correcta. Ya acordada con la compañera de BD.  
**⚠️ Importante:** No agregar Flyway. Ambas hacen lo mismo y tenerlas juntas genera conflicto al arrancar la aplicación.

---

### ✅ Lombok
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```
**Para qué sirve:** Genera en tiempo de compilación getters, setters y constructores mediante anotaciones.

| Anotación | Qué genera |
|---|---|
| `@Data` | Getters + Setters + toString + equals + hashCode |
| `@Getter` / `@Setter` | Solo getters o solo setters |
| `@NoArgsConstructor` | Constructor vacío |
| `@AllArgsConstructor` | Constructor con todos los campos |
| `@Builder` | Patrón Builder para crear objetos |

**Estado:** Recomendada. Reduce el código repetitivo en Models y DTOs.

---

### ✅ Spring Boot DevTools
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```
**Para qué sirve:** Reinicia el servidor automáticamente al detectar cambios en el código.  
**Estado:** Correcta solo para desarrollo. El `optional=true` garantiza que no se incluye en el jar de producción.

---

## Dependencia que se agrega manualmente

### ⚠️ JWT — jjwt versión 0.12.6
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```
**Para qué sirve:** Genera y valida tokens JWT. Al hacer login el backend genera un token firmado con `userId`, `email` y `rol`. El frontend lo envía en cada petición y el backend lo valida sin necesitar sesiones.  
**Estado:** Necesaria — agregar manualmente en el pom.xml.  
**⚠️ Versión corregida:** Usar **0.12.6** y NO 0.11.5. La 0.11.5 tiene toda la API deprecated.

---

## Dependencias que NO se agregan

### ❌ Flyway
**Razón:** El equipo trabaja con Liquibase. No pueden coexistir.

### ❌ MapStruct
**Razón:** Agrega complejidad de configuración sin beneficio real en esta etapa. El mapeo manual en el ServiceImpl es más claro ahora. Se puede evaluar más adelante cuando haya muchos módulos con conversiones repetitivas.

---

## Configuración application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/facelit_db
    username: postgres
    password: tu_password_aqui
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

server:
  port: 8080

jwt:
  secret: clave-secreta-muy-larga-de-al-menos-32-caracteres-para-HS256
  expiration: 28800000
```

**Notas:**
- `ddl-auto: validate` — Hibernate NO crea ni modifica tablas. Liquibase es quien maneja los cambios de BD. Hibernate solo verifica que el Model coincida con lo que Liquibase creó.
- `show-sql: true` — muestra el SQL generado en consola. Útil durante desarrollo.
- `liquibase.change-log` — debe coincidir con la ruta del archivo master que maneja la compañera de BD.
- `jwt.expiration: 28800000` — 8 horas en milisegundos.
- La clave JWT debe tener mínimo 32 caracteres. En producción va en variable de entorno, nunca en el código fuente.

---

## pom.xml completo y corregido

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>

    <groupId>com.FaceLit</groupId>
    <artifactId>backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>backend</name>
    <description>Sistema de reconocimiento facial SENA</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>

        <!-- WEB - endpoints REST y servidor Tomcat -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA + HIBERNATE - mapeo Model a BD -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- SEGURIDAD - BCrypt, JWT filter, roles -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- VALIDACIÓN - anotaciones en DTOs -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- BASE DE DATOS - driver PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- MIGRACIONES BD - Liquibase -->
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-core</artifactId>
        </dependency>

        <!-- JWT - autenticación stateless -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>

        <!-- LOMBOK - reduce boilerplate en Models y DTOs -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- DEVTOOLS - recarga automática en desarrollo -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- TESTS -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

---

## Correcciones aplicadas respecto a la configuración original

| Problema | Original | Corregido |
|---|---|---|
| Versión Spring Boot inestable | 4.0.6 | 3.4.1 |
| Versión JWT deprecated | 0.11.5 | 0.12.6 |
| Flyway junto a Liquibase | Ambas presentes | Solo Liquibase |
| MapStruct innecesario en esta etapa | Incluido | No incluido |

**Sin cambios — decisiones del equipo válidas:**
- Java 17 — correcto, compatibilidad total con todas las librerías
- Liquibase — correcto, ya acordado con la compañera de BD

---

*Documento del equipo FaceLit — Módulo auth HU-01*  
*Actualizar al agregar nuevas dependencias por módulo*
