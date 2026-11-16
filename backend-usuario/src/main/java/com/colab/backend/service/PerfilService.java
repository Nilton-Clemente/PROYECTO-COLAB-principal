package com.colab.backend.service;

import com.colab.backend.domain.Habilidad;
import com.colab.backend.domain.Usuario;
import com.colab.backend.domain.UsuarioHabilidad;
import com.colab.backend.domain.UsuarioHabilidadId;
import com.colab.backend.dto.HabilidadDto;
import com.colab.backend.dto.HabilidadPerfilDto;
import com.colab.backend.dto.PerfilResponse;
import com.colab.backend.repository.HabilidadRepository;
import com.colab.backend.repository.UsuarioHabilidadRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class PerfilService {

    private static final Set<String> NIVELES_VALIDOS = Set.of("BASICO", "INTERMEDIO", "AVANZADO");

    private final UsuarioRepository usuarioRepository;
    private final HabilidadRepository habilidadRepository;
    private final UsuarioHabilidadRepository usuarioHabilidadRepository;

    public PerfilService(UsuarioRepository usuarioRepository,
                         HabilidadRepository habilidadRepository,
                         UsuarioHabilidadRepository usuarioHabilidadRepository) {
        this.usuarioRepository = usuarioRepository;
        this.habilidadRepository = habilidadRepository;
        this.usuarioHabilidadRepository = usuarioHabilidadRepository;
    }

    public List<HabilidadDto> listarCatalogo() {
        return habilidadRepository.findAll().stream()
                .map(h -> new HabilidadDto(h.getId(), h.getNombre(), h.getArea(), h.getIcono()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil() {
        Usuario u = usuarioActual();
        List<HabilidadPerfilDto> habilidades = u.getHabilidades().stream()
                .map(uh -> new HabilidadPerfilDto(
                        uh.getHabilidad().getId(),
                        uh.getHabilidad().getNombre(),
                        uh.getHabilidad().getArea(),
                        uh.getNivel(),
                        uh.getHabilidad().getIcono()))
                .toList();
        return new PerfilResponse(u.getEmail(), u.getNombre(), habilidades);
    }

    @Transactional
    public void agregarHabilidad(Long habilidadId, String nivel) {
        Usuario u = usuarioActual();
        if (nivel != null && !NIVELES_VALIDOS.contains(nivel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nivel inválido. Valores permitidos: BASICO, INTERMEDIO, AVANZADO");
        }
        Habilidad h = habilidadRepository.findById(habilidadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habilidad no existe"));

        UsuarioHabilidadId id = new UsuarioHabilidadId(u.getId(), h.getId());
        UsuarioHabilidad uh = usuarioHabilidadRepository.findById(id).orElseGet(() -> {
            UsuarioHabilidad nueva = new UsuarioHabilidad();
            nueva.setId(id);
            nueva.setUsuario(u);
            nueva.setHabilidad(h);
            return nueva;
        });
        if (nivel != null) {
            uh.setNivel(nivel);
        }
        usuarioHabilidadRepository.save(uh);
    }

    @Transactional
    public void eliminarHabilidad(Long habilidadId) {
        Usuario u = usuarioActual();
        UsuarioHabilidadId id = new UsuarioHabilidadId(u.getId(), habilidadId);
        if (usuarioHabilidadRepository.existsById(id)) {
            usuarioHabilidadRepository.deleteById(id);
        }
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
