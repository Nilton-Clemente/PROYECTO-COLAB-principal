package com.colab.backend.controller;

import com.colab.backend.dto.*;
import com.colab.backend.service.AvanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}")
public class AvanceController {
    private final AvanceService avanceService;

    public AvanceController(AvanceService avanceService) {
        this.avanceService = avanceService;
    }

    @PostMapping("/tareas/{tareaId}/avances")
    public ResponseEntity<RegistroAvanceResponse> registrar(@PathVariable Long proyectoId, @PathVariable Long tareaId,
                                                           @Valid @RequestBody RegistroAvanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avanceService.registrar(proyectoId, tareaId, request));
    }

    @GetMapping("/tareas/{tareaId}/avances")
    public HistorialAvanceResponse historial(@PathVariable Long proyectoId, @PathVariable Long tareaId) {
        return avanceService.historial(proyectoId, tareaId);
    }

    @GetMapping("/metricas")
    public MetricasProyectoResponse metricas(@PathVariable Long proyectoId) {
        return avanceService.metricas(proyectoId);
    }
}