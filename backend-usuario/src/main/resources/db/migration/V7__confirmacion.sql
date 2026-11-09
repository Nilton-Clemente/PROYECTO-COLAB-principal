-- V6: Confirmaciones individuales de la propuesta (RF-20/RF-21/RF-22, RN-03/RN-04)
-- Entidad: confirmacion (15.15)

CREATE TABLE usuario.confirmacion (
    id               BIGSERIAL PRIMARY KEY,
    planificacion_id BIGINT NOT NULL REFERENCES usuario.planificacion (id) ON DELETE CASCADE,
    integrante_id    BIGINT NOT NULL REFERENCES usuario.integrante (id) ON DELETE CASCADE,
    estado           VARCHAR(50) NOT NULL,
    comentario       TEXT,
    fecha            TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT confirmacion_uniq UNIQUE (planificacion_id, integrante_id)
);
