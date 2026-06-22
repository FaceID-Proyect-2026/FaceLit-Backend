package com.FaceLit.backend.auth.repository.roleandpermission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    // Buscar e id del usuario para
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.idUser = :idUser")
    Optional<UserRole> findByUserId(@Param("idUser") UUID idUser);

}
