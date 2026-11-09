-- V7: Notificaciones in-app (RF-33/RF-34/RF-35, RN-22)
-- Entidad: notificacion (15.16)

CREATE TABLE usuario.notificacion (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT NOT NULL REFERENCES usuario.usuario (id) ON DELETE CASCADE,
    tipo_evento   VARCHAR(100) NOT NULL,
    mensaje       TEXT NOT NULL,
    leida         BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notificacion_usuario ON usuario.notificacion (usuario_id, leida);
