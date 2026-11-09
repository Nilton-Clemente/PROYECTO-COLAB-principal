package com.colab.backend.controller;

import com.colab.backend.dto.ConfirmacionResponse;
import com.colab.backend.dto.SolicitarCambiosRequest;
import com.colab.backend.service.ConfirmacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CU-10 — Confirmación individual de tareas, solicitud de cambios y activación
 * de la planificación.
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/planificacion")
public class ConfirmacionController {

    private final ConfirmacionService confirmacionService;

    public ConfirmacionController(ConfirmacionService confirmacionService) {
        this.confirmacionService = confirmacionService;
    }

    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmacionResponse> confirmar(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(confirmacionService.confirmar(proyectoId));
    }

    @PostMapping("/solicitar-cambios")
    public ResponseEntity<ConfirmacionResponse> solicitarCambios(
            @PathVariable Long proyectoId,
            @Valid @RequestBody SolicitarCambiosRequest request) {
        return ResponseEntity.ok(confirmacionService.solicitarCambios(proyectoId, request.comentario()));
    }

    @GetMapping("/confirmaciones")
    public List<ConfirmacionResponse> listar(@PathVariable Long proyectoId) {
        return confirmacionService.listar(proyectoId);
    }
}
