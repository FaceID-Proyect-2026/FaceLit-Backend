package com.FaceLit.backend.auth.repository.roleandpermission;

import org.springframework.data.jpa.repository.JpaRepository;

import com.FaceLit.backend.auth.model.enums.RoleName;
import java.util.Optional;
import com.FaceLit.backend.auth.model.roleandpermission.Role;
import java.util.UUID;

public interface RoleRepository extends  JpaRepository<Role, UUID>   {

     // Busca por nombre (nombre del rol) (Admin, Intructor, Aprendiz)
     // Rolename lo tengo definido en el model y tiene un Enum que me dice si es de rol administrador, intructor o aprendiz
    Optional<Role> findByNameRole(RoleName roleName); 

}
