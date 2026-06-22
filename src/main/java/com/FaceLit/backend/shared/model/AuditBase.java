package com.FaceLit.backend.shared.model;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.EntityListeners;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;


@MappedSuperclass  //  Anotacion que indica que voy a usar los atributos de esta clase,  pero no se va a crear en la base de datos. 
@EntityListeners(AuditingEntityListener.class) //  // Anotacion que indica que esta clase va a ser escuchada por el AuditingEntityListener, que es el encargado de llenar los campos de auditoria. 
@NoArgsConstructor  // Generar automaticamente los  contructores sin parametros
@Getter   // Generar automaticamente los Getter ( Obtener)
@Setter    //  Generar automaticamente los Setter ( Modificar)
    


public abstract class AuditBase {  // Auditoria Base

    @CreationTimestamp   // Fecha y hora en que el registro fue creado, Se asigna automáticamente al momento de persistir el registro.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp  // Fecha y hora de la última modificación del registro,   Se actualiza automáticamente cada vez que el registro cambia. 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Pendiente hasta que haya login con JWT — por ahora siempre null
    @Column(name = "created_by",  length = 100)

  // ID del usuario que creó el registro.
    private String createdBy;

    // Pendiente hasta que haya login con JWT — por ahora siempre null
    @Column(name = "updated_by",  length = 100)   // ID del usuario que realizó la última modificación.
    private String updatedBy;

    // Soft delete — null mientras el registro esté activo
    @Column(name = "deleted_at", length = 100)  // Fecha y hora en que el registro fue eliminado lógicamente (soft delete).
    private LocalDateTime deletedAt;

    // Soft delete — null mientras el registro esté activo
    @Column(name = "deleted_by", length = 100)   // ID del usuario que realizó la eliminación lógica.
    private String deletedBy;

}
