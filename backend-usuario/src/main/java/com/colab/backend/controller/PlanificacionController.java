package com.colab.backend.controller;

import com.colab.backend.dto.PlanificacionResponse;
import com.colab.backend.service.PlanificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU-08 — Generar y consultar la planificación de un proyecto.
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/planificacion")
public class PlanificacionController {

    private final PlanificacionService planificacionService;

    public PlanificacionController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @PostMapping
    public ResponseEntity<PlanificacionResponse> generar(@PathVariable Long proyectoId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planificacionService.generar(proyectoId));
    }

    @GetMapping
    public PlanificacionResponse ver(@PathVariable Long proyectoId) {
        return planificacionService.obtener(proyectoId);
    }
}
