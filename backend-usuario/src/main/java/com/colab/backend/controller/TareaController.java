package com.colab.backend.controller;

import com.colab.backend.dto.DependenciaRequest;
import com.colab.backend.dto.TareaDetalleResponse;
import com.colab.backend.dto.TareaEstadoRequest;
import com.colab.backend.dto.TareaRequest;
import com.colab.backend.service.TareaService;
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
 * Gestión manual de tareas (RF-23 a RF-32; CU-11/CU-12/CU-13).
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/tareas")
public class TareaController {

    private final TareaService tareaService;

    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    /** RF-32 — Todas las tareas del proyecto. */
    @GetMapping
    public List<TareaDetalleResponse> listar(@PathVariable Long proyectoId) {
        return tareaService.listarTodas(proyectoId);
    }

    /** RF-31 — Mis tareas. */
    @GetMapping("/mis-tareas")
    public List<TareaDetalleResponse> misTareas(@PathVariable Long proyectoId) {
        return tareaService.misTareas(proyectoId);
    }

    /** RF-23 — Crear tarea (solo Creador). */
    @PostMapping
    public ResponseEntity<TareaDetalleResponse> crear(@PathVariable Long proyectoId,
                                                      @Valid @RequestBody TareaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tareaService.crear(proyectoId, request));
    }

    /** RF-24 — Editar tarea (solo Creador). */
    @PutMapping("/{tareaId}")
    public TareaDetalleResponse editar(@PathVariable Long proyectoId,
                                       @PathVariable Long tareaId,
                                       @Valid @RequestBody TareaRequest request) {
        return tareaService.editar(proyectoId, tareaId, request);
    }

    /** RF-25 — Eliminar tarea (solo Creador). */
    @DeleteMapping("/{tareaId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long proyectoId, @PathVariable Long tareaId) {
        tareaService.eliminar(proyectoId, tareaId);
        return ResponseEntity.noContent().build();
    }

    /** RF-26 — Reasignar responsable (solo Creador). */
    @PutMapping("/{tareaId}/responsable/{responsableId}")
    public TareaDetalleResponse reasignar(@PathVariable Long proyectoId,
                                          @PathVariable Long tareaId,
                                          @PathVariable Long responsableId) {
        return tareaService.reasignar(proyectoId, tareaId, responsableId);
    }

    /** RF-28 — Cambiar estado (responsable o Creador). */
    @PutMapping("/{tareaId}/estado")
    public TareaDetalleResponse cambiarEstado(@PathVariable Long proyectoId,
                                              @PathVariable Long tareaId,
                                              @Valid @RequestBody TareaEstadoRequest request) {
        return tareaService.cambiarEstado(proyectoId, tareaId, request.nuevoEstado());
    }

    /** RF-27 — Agregar dependencia (solo Creador). */
    @PostMapping("/{tareaId}/dependencias")
    public TareaDetalleResponse agregarDependencia(@PathVariable Long proyectoId,
                                                   @PathVariable Long tareaId,
                                                   @Valid @RequestBody DependenciaRequest request) {
        return tareaService.agregarDependencia(proyectoId, tareaId, request.tareaPredecesoraId());
    }

    /** Quitar dependencia (solo Creador). */
    @DeleteMapping("/{tareaId}/dependencias/{predecesoraId}")
    public TareaDetalleResponse quitarDependencia(@PathVariable Long proyectoId,
                                                  @PathVariable Long tareaId,
                                                  @PathVariable Long predecesoraId) {
        return tareaService.quitarDependencia(proyectoId, tareaId, predecesoraId);
    }
}
