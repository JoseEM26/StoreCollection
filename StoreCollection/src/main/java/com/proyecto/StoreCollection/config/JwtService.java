// src/main/java/com/proyecto/StoreCollection/config/JwtService.java

package com.proyecto.StoreCollection.config;

import com.proyecto.StoreCollection.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24h por defecto
    private long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret no está configurado");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtService inicializado correctamente (secret: {} caracteres)", secret.length());
    }

    // GENERAR TOKEN CON TODOS LOS DATOS ÚTILES
    public String generateToken(UserDetails userDetails) {
        Usuario usuario = (Usuario) userDetails; // Tu clase ya es UserDetails

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", usuario.getId());
        claims.put("nombre", usuario.getNombre());
        claims.put("rol", usuario.getRol().name()); // ADMIN, OWNER, CUSTOMER
        claims.put("roles", usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

        // OPCIONAL: si tienes relación con tienda principal, agrégalo aquí
        // claims.put("tiendaId", usuario.getTiendaPrincipal() != null ? usuario.getTiendaPrincipal().getId() : null);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(usuario.getUsername()) // email
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .setId(java.util.UUID.randomUUID().toString()) // jti (para invalidar tokens)
                .signWith(key)
                .compact();
    }

    // === EXTRACCIÓN DE DATOS ===
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    public String extractNombre(String token) {
        return extractClaim(token, claims -> claims.get("nombre", String.class));
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    // Métodos genéricos
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}