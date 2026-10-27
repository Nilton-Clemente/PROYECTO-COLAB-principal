package com.colab.backend.controller;

import com.colab.backend.dto.InvitacionResponse;
import com.colab.backend.service.ProyectoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invitaciones")
public class InvitacionController {

    private final ProyectoService proyectoService;

    public InvitacionController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @GetMapping
    public List<InvitacionResponse> misInvitaciones() {
        return proyectoService.misInvitaciones();
    }

    @PostMapping("/{id}/aceptar")
    public ResponseEntity<Void> aceptar(@PathVariable Long id) {
        proyectoService.aceptarInvitacion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(@PathVariable Long id) {
        proyectoService.rechazarInvitacion(id);
        return ResponseEntity.noContent().build();
    }
}
