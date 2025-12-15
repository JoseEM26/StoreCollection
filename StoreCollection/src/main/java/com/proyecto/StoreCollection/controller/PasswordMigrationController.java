package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PasswordMigrationController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/api/public/hash-passwords")
    public ResponseEntity<String> hashPlainPasswords() {
        List<Usuario> usuarios = usuarioService.findAllRaw(); // Necesitas este método (ver abajo)

        int contador = 0;

        for (Usuario usuario : usuarios) {
            String passwordActual = usuario.getPassword();

            // Si NO empieza con $2a$ → es texto plano (BCrypt siempre empieza con $2a$)
            if (passwordActual == null || !passwordActual.startsWith("$2a$")) {
                String passwordHasheada = passwordEncoder.encode(passwordActual);
                usuario.setPassword(passwordHasheada);

                // Guardar sin pasar por validaciones de request DTO
                usuarioService.updatePasswordDirectly(usuario.getId(), passwordHasheada);

                log.info("Contraseña hasheada para usuario: {} (email: {})", usuario.getNombre(), usuario.getEmail());
                contador++;
            }
        }

        String mensaje = contador > 0
                ? "Se hashearon " + contador + " contraseñas en texto plano."
                : "No se encontró ninguna contraseña en texto plano. Todas ya están hasheadas.";

        log.info(mensaje);
        return ResponseEntity.ok(mensaje);
    }
}