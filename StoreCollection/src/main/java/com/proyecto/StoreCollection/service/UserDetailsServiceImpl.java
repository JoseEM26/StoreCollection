package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscamos por email (asumiendo que username = email)
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Construimos las autoridades/roles (ROLE_XXX)
        Collection<? extends GrantedAuthority> authorities = getAuthorities(usuario.getRol());

        return new User(
                usuario.getEmail(),                // username
                usuario.getPassword(),             // password hasheado
                usuario.isActivo(),                // isEnabled ← importante para bloquear inactivos
                true,                              // accountNonExpired
                true,                              // credentialsNonExpired
                true,                              // accountNonLocked
                authorities                        // ← Ahora sí está definido
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Usuario.Rol rol) {
        if (rol == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + rol.name())
        );
    }
}