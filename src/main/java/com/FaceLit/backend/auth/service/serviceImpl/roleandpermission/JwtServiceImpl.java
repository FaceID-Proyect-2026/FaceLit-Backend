package com.FaceLit.backend.auth.service.serviceImpl.roleandpermission;

import com.FaceLit.backend.auth.service.roleandpermission.JwtService;
import com.FaceLit.backend.auth.model.security.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    // Se lee desde application.yml — NUNCA hardcodeado
    @Value("${jwt.secret}")
    private String secret;

    // Se lee desde application.yml — ejemplo: 28800000 (8 horas en ms)
    @Value("${jwt.expiration}")
    private long expiration;

    // Convierte el secret en una clave segura para firmar el JWT
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(User user, String role, List<String> permissions) {
        return Jwts.builder()
                // userId — para identificar al usuario en cada request
                .claim("userId", user.getIdUser().toString())
                // email — útil para logs y auditoría
                .claim("email", user.getCredential().getEmail())
                // role — el frontend lo usa para redirigir al dashboard correcto
                .claim("role", role)
                // permissions — el backend los usa para proteger endpoints
                .claim("permissions", permissions)
                // subject — identificador principal del token
                .subject(user.getIdUser().toString())
                // fecha de emisión
                .issuedAt(new Date())
                // fecha de expiración — 8 horas desde ahora
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // firma con el secret
                .signWith(getKey())
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            // Si no lanza excepción, el token es válido y no expiró
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(getClaims(token).get("userId", String.class));
    }

    @Override
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return getClaims(token).get("permissions", List.class);
    }

    @Override
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    // Método interno — parsea y valida el token, extrae todos los claims
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Login exitoso

        // 1. Backend genera JWT con: { userId, email, role: "INSTRUCTOR", permissions:
        // [...] }
        // 2. Frontend guarda ese token
        // 3. Cada request siguiente lleva el token en el header: Authorization: Bearer
        // eyJhbGci...
        // 4. Backend lee el token → sabe quién es, qué rol tiene, qué puede hacer SIN
        // consultar la BD en cada request
    }

}