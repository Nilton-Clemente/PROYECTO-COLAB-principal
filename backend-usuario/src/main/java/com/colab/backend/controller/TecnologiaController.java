package com.colab.backend.controller;

import com.colab.backend.dto.TecnologiaDto;
import com.colab.backend.repository.TecnologiaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de tecnologías seleccionables para los proyectos.
 */
@RestController
@RequestMapping("/api/tecnologias")
public class TecnologiaController {

    private final TecnologiaRepository tecnologiaRepository;

    public TecnologiaController(TecnologiaRepository tecnologiaRepository) {
        this.tecnologiaRepository = tecnologiaRepository;
    }

    @GetMapping
    public List<TecnologiaDto> listar() {
        return tecnologiaRepository.findAll().stream()
                .map(t -> new TecnologiaDto(t.getId(), t.getNombre()))
                .toList();
    }
}
