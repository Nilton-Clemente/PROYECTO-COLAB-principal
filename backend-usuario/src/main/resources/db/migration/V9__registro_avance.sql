CREATE TABLE usuario.registro_avance (
    id BIGSERIAL PRIMARY KEY,
    tarea_id BIGINT NOT NULL REFERENCES usuario.tarea(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuario.usuario(id),
    minutos INTEGER NOT NULL CHECK (minutos > 0),
    fecha TIMESTAMP NOT NULL,
    nota VARCHAR(1000)
);
CREATE INDEX idx_registro_avance_tarea ON usuario.registro_avance(tarea_id);