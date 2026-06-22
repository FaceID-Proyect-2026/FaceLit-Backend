package com.FaceLit.backend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
// Aqui definimos las reglas de seguridad de nuestra API
// ES DECIR QUIEN PUEDE ENTRAR, A QUE PUEDE ACCEDER, COMO SE AUTENTICA
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Inyección por constructor — igual que en tus servicios
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // ─── PÚBLICOS — no necesitan token ───────────────────────
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/consent/**").permitAll()
                        .requestMatchers("/api/catalogos/**").permitAll()

                        // ─── SOLO ADMINISTRATOR ──────────────────────────────────
                        // Ver todos los usuarios, asignar roles, gestionar todo
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")

                        // ─── ADMINISTRATOR e INSTRUCTOR ──────────────────────────
                        // Ver asistencia de fichas, gestionar horarios
                        .requestMatchers("/api/instructor/**").hasAnyRole("ADMINISTRATOR", "INSTRUCTOR")

                        // ─── TODOS LOS ROLES AUTENTICADOS ────────────────────────
                        // Ver perfil propio, ver propia asistencia
                        .requestMatchers("/api/apprentice/**").hasAnyRole("ADMINISTRATOR", "INSTRUCTOR", "APPRENTICE")

                        // Cualquier otro endpoint requiere autenticación
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Agregar el JwtFilter ANTES del filtro de autenticación de Spring
                // Así Spring Security ya sabe quién es el usuario antes de verificar permisos
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}