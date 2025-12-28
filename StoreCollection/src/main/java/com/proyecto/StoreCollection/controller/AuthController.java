// src/main/java/com/proyecto/StoreCollection/Controller/AuthController.java
package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.*;
import com.proyecto.StoreCollection.config.JwtService;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import com.proyecto.StoreCollection.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    public record ErrorResponse(String code, String message) {}
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            Usuario usuario = usuarioRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            String token = jwtService.generateToken(usuario);

            return ResponseEntity.ok(new AuthResponse(
                    token, usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol()
            ));

        } catch (DisabledException e) {
            // Cuenta desactivada
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Cuenta desactivada",
                            "Tu cuenta está desactivada/inactiva. Contacta al administrador para reactivarla."));

        } catch (BadCredentialsException e) {
            // Contraseña incorrecta o email mal
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Credenciales inválidas",
                            "El correo electrónico o la contraseña son incorrectos."));

        } catch (UsernameNotFoundException e) {
            // Usuario no existe
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Usuario no encontrado",
                            "No existe una cuenta con ese correo electrónico."));

        } catch (Exception e) {
            log.error("Error inesperado en login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error del servidor", "Ocurrió un error inesperado. Intenta más tarde."));
        }
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