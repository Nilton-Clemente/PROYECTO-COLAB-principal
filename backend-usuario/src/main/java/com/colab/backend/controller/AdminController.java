package com.colab.backend.controller;

import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.ResetContrasenaRequest;
import com.colab.backend.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminApiKey;

    public AdminController(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${colab.admin.api-key:}") String adminApiKey) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminApiKey = adminApiKey;
    }

    @PutMapping("/usuarios/{id}/contrasena")
    public ResponseEntity<Void> restablecerContrasena(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @Valid @RequestBody ResetContrasenaRequest request) {
        verificarClaveAdmin(adminKey);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe"));
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuarioRepository.save(usuario);
        return ResponseEntity.noContent().build();
    }

    private void verificarClaveAdmin(String adminKey) {
        boolean valido = !adminApiKey.isBlank()
                && adminKey != null
                && MessageDigest.isEqual(
                        adminApiKey.getBytes(StandardCharsets.UTF_8),
                        adminKey.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clave de administración no válida");
        }
    }
}