package com.FaceLit.backend.auth.service.security;

import java.util.List;
import java.util.UUID;
import com.FaceLit.backend.auth.dto.request.security.UpdateUserRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.UserDetailResponseDTO;

public interface UserManagementService {

    // Lista todos los usuarios que han iniciado sesion al menos una vez
    List<UserDetailResponseDTO> getAllUsers();

    // Busca usuarios por nombre o correo (coincidencia parcial)
    List<UserDetailResponseDTO> searchUsers(String query);

    // Ver detalle completo de un usuario
    UserDetailResponseDTO getUserDetail(UUID userId);

    // Editar nombre, apellido, estado y rol
    UserDetailResponseDTO updateUser(UUID userId, UpdateUserRequestDTO dto);

    // Eliminar usuario — solo si no tiene dependencias activas
    void deleteUser(UUID userId);

}
