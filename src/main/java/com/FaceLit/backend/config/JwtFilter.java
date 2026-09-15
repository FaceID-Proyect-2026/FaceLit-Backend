package com.FaceLit.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/*
Este es el componente que intercepta cada request
antes de que llegue al controller, lee el token 
del header y le dice a Spring Security quién es
el usuario y qué permisos tiene.
*/

import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.FaceLit.backend.auth.service.roleandpermission.JwtService;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Component
public class JwtFilter extends OncePerRequestFilter {

    // OncePerRequestFilter garantiza que el filtro se ejecuta
    // exactamente UNA vez por request — nunca dos veces

    private final JwtService jwtService;
    private final UserRoleRepository userRoleRepository;

    public JwtFilter(JwtService jwtService, UserRoleRepository userRoleRepository) {
        this.jwtService = jwtService;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Leer el header Authorization del request
        // El frontend manda: Authorization: Bearer eyJhbGci...
        String authHeader = request.getHeader("Authorization");

        // 2. Si no viene el header o no empieza con "Bearer ", dejar pasar
        // Los endpoints públicos (login, register) no mandan token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token — quitar el prefijo "Bearer "
        String token = authHeader.substring(7);

        // 4. Validar el token — firma correcta y no expirado
        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Resolver el rol y los permisos actuales desde la BD.
        // El JWT identifica al usuario, pero no congela su autorizacion.
        java.util.UUID userId = jwtService.extractUserId(token);
        List<Object[]> currentAuthorities = userRoleRepository.findCurrentAuthorities(userId);
        if (currentAuthorities.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String role = currentAuthorities.get(0)[0].toString();
        List<String> permissions = currentAuthorities.stream()
            .map(authority -> authority[1].toString())
            .distinct()
            .toList();

        // 6. Construir las autoridades que Spring Security entiende
        // El rol lleva prefijo ROLE_ — así funciona hasRole() en Spring
        // Los permisos van directo — así funciona hasAuthority()
        //
        // Ejemplo resultado:
        // [ROLE_INSTRUCTOR, VIEW_OWN_PROFILE, VIEW_ATTENDANCE]
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        authorities.addAll(permissions.stream().map(SimpleGrantedAuthority::new).toList());

        // 7. Crear el objeto de autenticación para Spring Security
        // null en las credenciales — ya no las necesitamos, el token es suficiente
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                jwtService.extractUserId(token), // principal = userId
                null, // credentials = null
                authorities // roles y permisos
        );

        // 8. Registrar la autenticación en el contexto de seguridad
        // A partir de aquí Spring Security sabe quién es el usuario
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 9. Continuar con el request
        filterChain.doFilter(request, response);
    }

}