package com.colab.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_habilidad")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioHabilidad {

    @EmbeddedId
    private UsuarioHabilidadId id;

    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @MapsId("habilidadId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidad_id")
    private Habilidad habilidad;

    @Column(name = "nivel", length = 50)
    private String nivel;
}
