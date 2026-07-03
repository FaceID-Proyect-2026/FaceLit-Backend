package com.FaceLit.backend.auth.service.serviceImpl.roleandpermission;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.FaceLit.backend.auth.dto.response.security.UserListResponseDTO;
import com.FaceLit.backend.auth.model.security.User;
import com.FaceLit.backend.auth.dto.request.roleandpermission.AssignRoleRequestDTO;
import com.FaceLit.backend.auth.dto.response.roleandpermission.AssignRoleResponseDTO;
import com.FaceLit.backend.auth.exception.LoginException;
import com.FaceLit.backend.auth.model.roleandpermission.Role;
import com.FaceLit.backend.auth.model.roleandpermission.UserRole;
import com.FaceLit.backend.auth.repository.roleandpermission.RoleRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminRoleServiceImpl implements AdminRoleService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CredentialRepository credentialRepository;

    public AdminRoleServiceImpl(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            CredentialRepository credentialRepository) {

        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public List<UserListResponseDTO> getAllUsers() {

        // 1. Traer todos los usuarios de BD
        List<User> users = userRepository.findAll();

        // 2. Convertir cada User a UserListResponseDTO
        return users.stream().map(user -> {

            // 3. Obtener el email desde Credential
            String email = credentialRepository.findByUser(user)
                    .map(c -> c.getEmail())
                    .orElse("Sin correo");

            // 4. Obtener el rol actual desde UserRole
            String currentRole = userRoleRepository.findByUserId(user.getIdUser())
                    .map(ur -> ur.getRole().getNameRole().name())
                    .orElse("Sin rol");

            return new UserListResponseDTO(
                    user.getIdUser(),
                    user.getFirstName(),
                    user.getLastName(),
                    email,
                    user.getDocumentNumber(),
                    currentRole
            );
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssignRoleResponseDTO assignRole(UUID userId, AssignRoleRequestDTO dto) {

        // 1. Verificar que el usuario existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LoginException("Usuario no encontrado"));

        // 2. Buscar el rol que se quiere asignar
        Role newRole = roleRepository.findByNameRole(dto.getRole())
                .orElseThrow(() -> new LoginException("Rol no encontrado"));

        // 3. Buscar si ya tiene un rol asignado
        // Si ya tiene uno → actualizarlo
        // Si no tiene → crear uno nuevo
        UserRole userRole = userRoleRepository.findByUserId(userId)
                .orElse(new UserRole());

        userRole.setUser(user);
        userRole.setRole(newRole);
        userRole.setAssignmentDate(LocalDate.now());
        userRole.setAssignedAt(OffsetDateTime.now());
        userRoleRepository.save(userRole);

        // 4. Responder con el userId, el nuevo rol y mensaje de éxito
        return AssignRoleResponseDTO.success(userId, newRole.getNameRole().name());
    }
}