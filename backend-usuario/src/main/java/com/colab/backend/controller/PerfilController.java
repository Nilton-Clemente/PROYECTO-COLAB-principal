package com.colab.backend.controller;

import com.colab.backend.dto.HabilidadDto;
import com.colab.backend.dto.PerfilResponse;
import com.colab.backend.service.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/habilidades")
    public List<HabilidadDto> listarCatalogo() {
        return perfilService.listarCatalogo();
    }

    @GetMapping("/usuario/perfil")
    public PerfilResponse obtenerPerfil() {
        return perfilService.obtenerPerfil();
    }

    @PutMapping("/usuario/habilidades/{habilidadId}")
    public ResponseEntity<Void> agregarHabilidad(@PathVariable Long habilidadId,
                                                 @RequestParam(required = false) String nivel) {
        perfilService.agregarHabilidad(habilidadId, nivel);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/usuario/habilidades/{habilidadId}")
    public ResponseEntity<Void> eliminarHabilidad(@PathVariable Long habilidadId) {
        perfilService.eliminarHabilidad(habilidadId);
        return ResponseEntity.noContent().build();
    }
}
