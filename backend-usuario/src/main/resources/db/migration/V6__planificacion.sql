-- V3: Planificación generada por IA (CU-08)
-- Entidades: planificacion, etapa, tarea y dependencia_tarea (RN-19 / RN-20)

CREATE TABLE usuario.planificacion (
    id               BIGSERIAL PRIMARY KEY,
    proyecto_id      BIGINT NOT NULL UNIQUE REFERENCES usuario.proyecto (id) ON DELETE CASCADE,
    estado           VARCHAR(50) NOT NULL,
    fecha_generacion TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE usuario.etapa (
    id               BIGSERIAL PRIMARY KEY,
    planificacion_id BIGINT NOT NULL REFERENCES usuario.planificacion (id) ON DELETE CASCADE,
    nombre           VARCHAR(255) NOT NULL,
    orden            INTEGER
);

CREATE TABLE usuario.tarea (
    id             BIGSERIAL PRIMARY KEY,
    etapa_id       BIGINT NOT NULL REFERENCES usuario.etapa (id) ON DELETE CASCADE,
    nombre         VARCHAR(255) NOT NULL,
    descripcion    TEXT,
    responsable_id BIGINT REFERENCES usuario.integrante (id) ON DELETE SET NULL,
    prioridad      VARCHAR(50) NOT NULL,
    estado         VARCHAR(50) NOT NULL,
    duracion_dias  INTEGER NOT NULL CHECK (duracion_dias > 0),
    fecha_inicio   DATE,
    fecha_fin      DATE,
    orden          INTEGER
);

CREATE TABLE usuario.dependencia_tarea (
    tarea_id             BIGINT NOT NULL REFERENCES usuario.tarea (id) ON DELETE CASCADE,
    tarea_predecesora_id BIGINT NOT NULL REFERENCES usuario.tarea (id) ON DELETE CASCADE,
    PRIMARY KEY (tarea_id, tarea_predecesora_id)
);
