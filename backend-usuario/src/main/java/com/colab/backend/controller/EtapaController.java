package com.colab.backend.controller;

import com.colab.backend.dto.EtapaRequest;
import com.colab.backend.dto.EtapaResponse;
import com.colab.backend.service.EtapaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestión manual de fases (etapas) de un proyecto.
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/etapas")
public class EtapaController {

    private final EtapaService etapaService;

    public EtapaController(EtapaService etapaService) {
        this.etapaService = etapaService;
    }

    /** Listar todas las fases del proyecto. */
    @GetMapping
    public List<EtapaResponse> listar(@PathVariable Long proyectoId) {
        return etapaService.listar(proyectoId);
    }

    /** Obtener una fase concreta del proyecto. */
    @GetMapping("/{etapaId}")
    public EtapaResponse obtener(@PathVariable Long proyectoId, @PathVariable Long etapaId) {
        return etapaService.obtener(proyectoId, etapaId);
    }

    /** Crear una fase (solo Creador). */
    @PostMapping
    public ResponseEntity<EtapaResponse> crear(@PathVariable Long proyectoId,
                                               @Valid @RequestBody EtapaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etapaService.crear(proyectoId, request));
    }

    /** Editar una fase (solo Creador). */
    @PutMapping("/{etapaId}")
    public EtapaResponse editar(@PathVariable Long proyectoId,
                                @PathVariable Long etapaId,
                                @Valid @RequestBody EtapaRequest request) {
        return etapaService.editar(proyectoId, etapaId, request);
    }

    /** Eliminar una fase (solo Creador). */
    @DeleteMapping("/{etapaId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long proyectoId, @PathVariable Long etapaId) {
        etapaService.eliminar(proyectoId, etapaId);
        return ResponseEntity.noContent().build();
    }
}
