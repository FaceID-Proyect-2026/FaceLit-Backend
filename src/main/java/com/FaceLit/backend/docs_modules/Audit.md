# Auditoría en el backend — FaceLit
**Módulo:** `shared/model/AuditoriaBase.java`  
**Aplica a:** todas las entidades del proyecto  

---

## ¿Qué es la auditoría en una base de datos?

La auditoría es un mecanismo que registra automáticamente **quién hizo qué y cuándo** sobre cada registro de la base de datos. En vez de que un registro desaparezca para siempre cuando alguien lo "borra", el sistema lo marca como inactivo pero lo conserva. Esto se llama **borrado lógico** o *soft delete*.

Dicho de forma simple: los datos nunca se eliminan físicamente. Siempre hay trazabilidad completa de cada acción.

---

## ¿Por qué aplicarlo en FaceLit?

El proyecto maneja datos personales de usuarios, menores de edad y consentimientos. La Ley 1581 de 2012 (que ya está citada en los requerimientos del proyecto) exige trazabilidad del tratamiento de datos personales. La auditoría cubre eso automáticamente sin que tengas que escribir lógica extra en cada endpoint.

Además, si en algún momento hay un error o un reclamo, puedes saber exactamente quién creó un registro, quién lo modificó y quién lo eliminó.

---

## Los 6 campos de auditoría

Estos 6 campos van en **todas** las entidades del proyecto heredados desde `AuditoriaBase`.

| Campo | Tipo Java | Tipo PostgreSQL | Se llena automáticamente |
|---|---|---|---|
| `createdAt` | `LocalDateTime` | `TIMESTAMP` | Sí — al crear el registro |
| `updatedAt` | `LocalDateTime` | `TIMESTAMP` | Sí — al modificar el registro |
| `createdBy` | `Long` | `BIGINT` | No todavía — requiere JWT |
| `updatedBy` | `Long` | `BIGINT` | No todavía — requiere JWT |
| `deletedAt` | `LocalDateTime` | `TIMESTAMP` | No — se llena al hacer soft delete |
| `deletedBy` | `Long` | `BIGINT` | No — se llena al hacer soft delete |

---

## Campos activos ahora vs campos para más adelante

### ✅ Activos desde HU-01

**`createdAt`** — se llena solo con `@CreationTimestamp` de Hibernate en el momento en que el registro se guarda por primera vez. No necesitas escribir nada en el Service.

**`updatedAt`** — se actualiza solo con `@UpdateTimestamp` cada vez que el registro se modifica. Tampoco necesitas código extra.

Estos dos funcionan desde el primer momento porque no necesitan saber quién es el usuario — Hibernate los maneja internamente.

---

### ⏳ Pendientes para cuando esté el login (JWT)

**`createdBy`** — guarda el `idUsuario` de quien creó el registro. Para saber quién es, necesitas leer el token JWT del request. Como en HU-01 de registro el usuario aún no está autenticado (está creando su cuenta), este campo queda en `null` por ahora.

**`updatedBy`** — guarda el `idUsuario` de quien hizo la última modificación. Mismo caso — requiere JWT activo.

Cuando implementes el login en HU-01 de autenticación, activarás estos dos campos usando `SpringSecurityAuditorAware`, que es una clase que le dice a Spring cómo obtener el usuario actual desde el contexto de seguridad.

```
⚠️ Pendiente HU-01 login:
Crear la clase AuditorAware que lea el idUsuario del JWT
y registrarlo como @Bean en SecurityConfig.
```

---

### 🗑️ Soft delete — para cuando se necesite borrar algo

**`deletedAt`** — fecha y hora en que el registro fue "borrado". Cuando esto tiene un valor, el registro se considera eliminado. Cuando es `null`, el registro está activo.

**`deletedBy`** — `idUsuario` de quien hizo el borrado.

El soft delete significa que **nunca usas `repository.delete()`**. En cambio, en el Service haces:

```java
usuario.setDeletedAt(LocalDateTime.now());
usuario.setDeletedBy(idDelUsuarioActual);
usuarioRepository.save(usuario);
```

Y para que los queries no devuelvan registros "borrados", se usa la anotación `@Where` en la entidad o filtros en el Repository. Esto se implementa cuando llegue el módulo que lo necesite.

---

## La clase AuditoriaBase

```java
// shared/model/AuditoriaBase.java

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditoriaBase {

    // ✅ ACTIVO DESDE HU-01 — Hibernate lo llena automáticamente
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // ✅ ACTIVO DESDE HU-01 — Hibernate lo actualiza automáticamente
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ⏳ PENDIENTE LOGIN — queda null hasta que haya JWT
    @Column(name = "created_by", nullable = true)
    private Long createdBy;

    // ⏳ PENDIENTE LOGIN — queda null hasta que haya JWT
    @Column(name = "updated_by", nullable = true)
    private Long updatedBy;

    // 🗑️ SOFT DELETE — queda null mientras el registro esté activo
    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    // 🗑️ SOFT DELETE — queda null mientras el registro esté activo
    @Column(name = "deleted_by", nullable = true)
    private Long deletedBy;

    // getters y setters de los 6 campos
}
```

**`@MappedSuperclass`** — le dice a JPA que esta clase no genera una tabla propia. Sus campos se "copian" a cada tabla que la extienda.

**`@EntityListeners(AuditingEntityListener.class)`** — activa el listener de Spring que detecta cuándo se crea o modifica un registro y dispara los timestamps automáticos.

---

## Activar la auditoría en el proyecto

En la clase principal del proyecto agregar esta anotación:

```java
// BackendApplication.java

@SpringBootApplication
@EnableJpaAuditing   // ← activa @CreationTimestamp y @UpdateTimestamp en todo el proyecto
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```


## Estructura de carpetas actualizada

```
shared/
├── model/
│   └── AuditoriaBase.java    ← clase base que extienden TODOS los models
└── exception/
    └── GlobalExceptionHandler.java

auth/
└── model/
    ├── Usuario.java          ← extends AuditoriaBase
    ├── Credencial.java       ← extends AuditoriaBase
    ├── Acudiente.java        ← extends AuditoriaBase (HU-03)
    ├── Consentimiento.java   ← extends AuditoriaBase (HU-04)
    ├── Verificacion.java     ← extends AuditoriaBase (HU-04)
    └── enums/
        ├── EstadoCuenta.java
        ├── EstadoCredencial.java
        └── EstadoConsentimiento.java
```

---

## Roadmap de la auditoría

```
HU-01 registro   → createdAt y updatedAt funcionando ✅
HU-01 login      → activar createdBy y updatedBy con JWT ⏳
Módulos de CRUD  → implementar soft delete (deletedAt, deletedBy) 🗑️
```

---

*Documento del equipo FaceLit — Módulo shared*  
*Actualizar cuando se active createdBy/updatedBy en el login*