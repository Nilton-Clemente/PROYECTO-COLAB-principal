package com.colab.backend.service;

import com.colab.backend.domain.CasoUso;
import com.colab.backend.domain.Integrante;
import com.colab.backend.domain.Proyecto;
import com.colab.backend.domain.RequerimientoFuncional;
import com.colab.backend.domain.RequerimientoNoFuncional;
import com.colab.backend.domain.Tecnologia;
import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.CasoUsoDto;
import com.colab.backend.dto.IntegranteResponse;
import com.colab.backend.dto.InvitacionResponse;
import com.colab.backend.dto.ProyectoRequest;
import com.colab.backend.dto.ProyectoResponse;
import com.colab.backend.dto.RequerimientoFuncionalDto;
import com.colab.backend.dto.RequerimientoNoFuncionalDto;
import com.colab.backend.dto.TecnologiaDto;
import com.colab.backend.repository.IntegranteRepository;
import com.colab.backend.repository.ProyectoRepository;
import com.colab.backend.repository.TecnologiaRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final IntegranteRepository integranteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TecnologiaRepository tecnologiaRepository;

    public ProyectoService(ProyectoRepository proyectoRepository,
                           IntegranteRepository integranteRepository,
                           UsuarioRepository usuarioRepository,
                           TecnologiaRepository tecnologiaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.integranteRepository = integranteRepository;
        this.usuarioRepository = usuarioRepository;
        this.tecnologiaRepository = tecnologiaRepository;
    }

    @Transactional
    public ProyectoResponse crearProyecto(ProyectoRequest request) {
        Usuario u = usuarioActual();

        Proyecto p = new Proyecto();
        aplicarRequest(p, request);
        proyectoRepository.save(p);

        // El creador queda registrado como integrante con rol CREADOR.
        Integrante creador = new Integrante();
        creador.setUsuario(u);
        creador.setProyecto(p);
        creador.setRol("CREADOR");
        creador.setEstado("ACTIVO");
        creador.setFechaUnion(LocalDateTime.now());
        integranteRepository.save(creador);

        return aResponse(p);
    }

    @Transactional
    public ProyectoResponse editarProyecto(Long id, ProyectoRequest request) {
        Usuario u = usuarioActual();
        Proyecto p = proyectoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no existe"));
        verificarCreador(u, id);

        aplicarRequest(p, request);
        proyectoRepository.save(p);
        return aResponse(p);
    }

    @Transactional
    public void eliminarProyecto(Long id) {
        Usuario u = usuarioActual();
        Proyecto p = proyectoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no existe"));
        verificarCreador(u, id);
        proyectoRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public List<ProyectoResponse> listarMisProyectos() {
        Usuario u = usuarioActual();
        return integranteRepository.findByUsuarioId(u.getId()).stream()
                .map(Integrante::getProyecto)
                .map(this::aResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProyectoResponse verProyecto(Long id) {
        Usuario u = usuarioActual();
        Proyecto p = proyectoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no existe"));

        if (!integranteRepository.existsByUsuarioIdAndProyectoId(u.getId(), id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este proyecto");
        }
        return aResponse(p);
    }

    @Transactional(readOnly = true)
    public List<IntegranteResponse> verEquipo(Long proyectoId) {
        Usuario u = usuarioActual();
        verificarMiembro(u, proyectoId);
        return integranteRepository.findByProyectoId(proyectoId).stream()
                .map(i -> new IntegranteResponse(
                        i.getId(),
                        i.getUsuario().getEmail(),
                        i.getUsuario().getNombre(),
                        i.getRol(),
                        i.getEstado()))
                .toList();
    }

    @Transactional
    public void invitar(Long proyectoId, String email) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);

        Usuario invitado = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El correo no corresponde a un usuario registrado"));
        if (integranteRepository.existsByUsuarioIdAndProyectoId(invitado.getId(), proyectoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya pertenece a este proyecto");
        }
        Proyecto p = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no existe"));

        Integrante i = new Integrante();
        i.setUsuario(invitado);
        i.setProyecto(p);
        i.setRol("INTEGRANTE");
        i.setEstado("PENDIENTE");
        integranteRepository.save(i);
    }

    @Transactional
    public void eliminarIntegrante(Long proyectoId, Long integranteId) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Integrante i = integranteRepository.findById(integranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integrante no existe"));
        if (!i.getProyecto().getId().equals(proyectoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El integrante no pertenece a este proyecto");
        }
        if ("CREADOR".equals(i.getRol())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar al Creador");
        }
        integranteRepository.delete(i);
    }

    @Transactional(readOnly = true)
    public List<InvitacionResponse> misInvitaciones() {
        Usuario u = usuarioActual();
        return integranteRepository.findByUsuarioIdAndEstado(u.getId(), "PENDIENTE").stream()
                .map(i -> new InvitacionResponse(i.getId(), i.getProyecto().getId(), i.getProyecto().getNombre()))
                .toList();
    }

    @Transactional
    public void aceptarInvitacion(Long integranteId) {
        Usuario u = usuarioActual();
        Integrante i = integranteRepository.findById(integranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitación no existe"));
        if (!i.getUsuario().getId().equals(u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta invitación no es tuya");
        }
        if (!"PENDIENTE".equals(i.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La invitación ya fue procesada");
        }
        i.setEstado("ACTIVO");
        i.setFechaUnion(LocalDateTime.now());
        integranteRepository.save(i);
    }

    @Transactional
    public void rechazarInvitacion(Long integranteId) {
        Usuario u = usuarioActual();
        Integrante i = integranteRepository.findById(integranteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitación no existe"));
        if (!i.getUsuario().getId().equals(u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta invitación no es tuya");
        }
        integranteRepository.delete(i);
    }

    private void aplicarRequest(Proyecto p, ProyectoRequest request) {
        p.setNombre(request.nombre());
        p.setDescripcion(request.descripcion());
        p.setProblematica(request.problematica());
        p.setObjetivoGeneral(request.objetivoGeneral());
        p.setObjetivosEspecificos(request.objetivosEspecificos());
        p.setAlcance(request.alcance());
        p.setRestricciones(request.restricciones());
        p.setFechaEntrega(request.fechaEntrega());

        // Tecnologias (N:M)
        if (request.tecnologiasIds() != null && !request.tecnologiasIds().isEmpty()) {
            List<Tecnologia> tecnologias = tecnologiaRepository.findAllById(request.tecnologiasIds());
            if (tecnologias.size() != request.tecnologiasIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una o más tecnologías no existen");
            }
            p.getTecnologias().clear();
            p.getTecnologias().addAll(tecnologias);
        } else {
            p.getTecnologias().clear();
        }

        // Requerimientos funcionales (1:N)
        p.getRequerimientosFuncionales().clear();
        if (request.requerimientosFuncionales() != null) {
            for (RequerimientoFuncionalDto dto : request.requerimientosFuncionales()) {
                RequerimientoFuncional rf = new RequerimientoFuncional();
                rf.setProyecto(p);
                rf.setCodigo(dto.codigo());
                rf.setDescripcion(dto.descripcion());
                p.getRequerimientosFuncionales().add(rf);
            }
        }

        // Requerimientos no funcionales (1:N)
        p.getRequerimientosNoFuncionales().clear();
        if (request.requerimientosNoFuncionales() != null) {
            for (RequerimientoNoFuncionalDto dto : request.requerimientosNoFuncionales()) {
                RequerimientoNoFuncional rnf = new RequerimientoNoFuncional();
                rnf.setProyecto(p);
                rnf.setCodigo(dto.codigo());
                rnf.setCategoria(dto.categoria());
                rnf.setDescripcion(dto.descripcion());
                p.getRequerimientosNoFuncionales().add(rnf);
            }
        }

        // Casos de uso (1:N)
        p.getCasosDeUso().clear();
        if (request.casosDeUso() != null) {
            for (CasoUsoDto dto : request.casosDeUso()) {
                CasoUso cu = new CasoUso();
                cu.setProyecto(p);
                cu.setCodigo(dto.codigo());
                cu.setNombre(dto.nombre());
                cu.setDescripcion(dto.descripcion());
                p.getCasosDeUso().add(cu);
            }
        }
    }

    private ProyectoResponse aResponse(Proyecto p) {
        return new ProyectoResponse(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getProblematica(),
                p.getObjetivoGeneral(),
                p.getObjetivosEspecificos(),
                p.getAlcance(),
                p.getRestricciones(),
                p.getFechaEntrega(),
                p.getTipo(),
                p.getTecnologias().stream()
                        .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                        .map(t -> new TecnologiaDto(t.getId(), t.getNombre()))
                        .toList(),
                p.getRequerimientosFuncionales().stream()
                        .map(rf -> new RequerimientoFuncionalDto(rf.getDescripcion(), rf.getCodigo()))
                        .toList(),
                p.getRequerimientosNoFuncionales().stream()
                        .map(rnf -> new RequerimientoNoFuncionalDto(rnf.getDescripcion(), rnf.getCodigo(), rnf.getCategoria()))
                        .toList(),
                p.getCasosDeUso().stream()
                        .map(cu -> new CasoUsoDto(cu.getNombre(), cu.getCodigo(), cu.getDescripcion()))
                        .toList());
    }

    private void verificarCreador(Usuario u, Long proyectoId) {
        integranteRepository.findByUsuarioIdAndProyectoId(u.getId(), proyectoId)
                .filter(i -> "CREADOR".equals(i.getRol()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Creador puede realizar esta acción"));
    }

    private void verificarMiembro(Usuario u, Long proyectoId) {
        if (!integranteRepository.existsByUsuarioIdAndProyectoId(u.getId(), proyectoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este proyecto");
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
