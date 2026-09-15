package com.FaceLit.backend.auth.service.security;

import com.FaceLit.backend.auth.dto.request.security.RegisterRequestDTO;
import com.FaceLit.backend.auth.dto.response.security.RegisterResponseDTO;

public interface RegisterService {
    // impementamos un metodo
    // RegisterResponseDTO = cuando termina el registro, devuelve informacion
    // register = nombre del metodo
    // RegisterRequestDTO = datos que envia el usuario
    // Recibe los datos del formulario
    RegisterResponseDTO register(RegisterRequestDTO dto);

}
