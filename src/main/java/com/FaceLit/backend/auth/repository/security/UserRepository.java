package com.FaceLit.backend.auth.repository.security;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.FaceLit.backend.auth.model.security.User;

// EL JpaRespository es  una interfas que proporciona metodos para relacionar con la base de datos. 
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // User es la entidad Y login es el tipo de dato del ID (Long)

    // aqui lo que hace es verificar si existe un usuario con ese número de
    // documento
    boolean existsByDocumentNumber(String documentNumber);

    // -- PARA RECOERDAR ---- //

    // solo se necesita definir el metodo, existsByDocumentNumber (verificar si
    // existe numero de documento)
    // no pone mas metodos porque el JpaRepository ya tiene metodos predefinidos
    // como save, findById, deleteById

    Optional<User> findByDocumentNumber(String documentNumber);

}
