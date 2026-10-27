package com.colab.backend.controller;

import com.colab.backend.dto.IntegranteResponse;
import com.colab.backend.dto.InvitacionRequest;
import com.colab.backend.dto.ProyectoRequest;
import com.colab.backend.dto.ProyectoResponse;
import com.colab.backend.service.ProyectoService;
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

@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @PostMapping
    public ResponseEntity<ProyectoResponse> crear(@Valid @RequestBody ProyectoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proyectoService.crearProyecto(request));
    }

    @GetMapping
    public List<ProyectoResponse> listarMisProyectos() {
        return proyectoService.listarMisProyectos();
    }

    @GetMapping("/{id}")
    public ProyectoResponse ver(@PathVariable Long id) {
        return proyectoService.verProyecto(id);
    }

    @PutMapping("/{id}")
    public ProyectoResponse editar(@PathVariable Long id, @Valid @RequestBody ProyectoRequest request) {
        return proyectoService.editarProyecto(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        proyectoService.eliminarProyecto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/integrantes")
    public List<IntegranteResponse> verEquipo(@PathVariable Long id) {
        return proyectoService.verEquipo(id);
    }

    @PostMapping("/{id}/invitaciones")
    public ResponseEntity<Void> invitar(@PathVariable Long id, @Valid @RequestBody InvitacionRequest request) {
        proyectoService.invitar(id, request.email());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/integrantes/{integranteId}")
    public ResponseEntity<Void> eliminarIntegrante(@PathVariable Long id, @PathVariable Long integranteId) {
        proyectoService.eliminarIntegrante(id, integranteId);
        return ResponseEntity.noContent().build();
    }
}
