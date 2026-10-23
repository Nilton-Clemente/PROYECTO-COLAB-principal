package com.colab.backend.controller;

import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.AuthResponse;
import com.colab.backend.dto.LoginRequest;
import com.colab.backend.dto.RegistroRequest;
import com.colab.backend.repository.UsuarioRepository;
import com.colab.backend.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().build();
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuario.setNombre(request.nombre());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(new AuthResponse(jwtService.generarToken(usuario.getEmail())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return usuarioRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.contrasena(), u.getContrasenaHash()))
                .map(u -> ResponseEntity.ok(new AuthResponse(jwtService.generarToken(u.getEmail()))))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
