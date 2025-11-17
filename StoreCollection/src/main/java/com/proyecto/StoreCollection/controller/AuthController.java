// src/main/java/com/proyecto/StoreCollection/Controller/AuthController.java
package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.*;
import com.proyecto.StoreCollection.config.JwtService;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import com.proyecto.StoreCollection.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Intento de login: {}", request.email());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateToken(usuario);

        return ResponseEntity.ok(new AuthResponse(
                token, usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("Registro nuevo: {}", request.email());

        if (usuarioRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body(null);
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .celular(request.celular())
                .rol(Usuario.Rol.CUSTOMER)
                .build();

        usuarioRepository.save(usuario);
        String token = jwtService.generateToken(usuario);

        return ResponseEntity.ok(new AuthResponse(
                token, usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol()
        ));
    }
}