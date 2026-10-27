-- V2: Proyectos y equipo de trabajo

CREATE TABLE usuario.proyecto (
    id                    BIGSERIAL PRIMARY KEY,
    nombre                VARCHAR(255) NOT NULL,
    descripcion           TEXT,
    problematica          TEXT,
    objetivo_general      TEXT,
    objetivos_especificos TEXT,
    alcance               TEXT,
    restricciones         TEXT,
    fecha_entrega         DATE,
    fecha_creacion        TIMESTAMP NOT NULL DEFAULT now(),
    tipo                  VARCHAR(50) NOT NULL DEFAULT 'SOFTWARE'
);

CREATE TABLE usuario.integrante (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuario.usuario (id) ON DELETE CASCADE,
    proyecto_id BIGINT NOT NULL REFERENCES usuario.proyecto (id) ON DELETE CASCADE,
    rol         VARCHAR(50) NOT NULL,
    estado      VARCHAR(50) NOT NULL,
    fecha_union TIMESTAMP,
    CONSTRAINT integrante_uniq UNIQUE (usuario_id, proyecto_id)
);
