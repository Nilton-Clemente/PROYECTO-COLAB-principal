-- V4: Contenido estructurado del proyecto (tecnologias, RF, RNF y casos de uso)
-- Entidades: tecnologia (15.5), proyecto_tecnologia (15.6),
-- requerimiento_funcional (15.7), requerimiento_no_funcional (15.8) y caso_uso (15.9)

CREATE TABLE usuario.tecnologia (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE usuario.proyecto_tecnologia (
    proyecto_id   BIGINT NOT NULL REFERENCES usuario.proyecto (id) ON DELETE CASCADE,
    tecnologia_id BIGINT NOT NULL REFERENCES usuario.tecnologia (id) ON DELETE CASCADE,
    PRIMARY KEY (proyecto_id, tecnologia_id)
);

CREATE TABLE usuario.requerimiento_funcional (
    id          BIGSERIAL PRIMARY KEY,
    proyecto_id BIGINT NOT NULL REFERENCES usuario.proyecto (id) ON DELETE CASCADE,
    codigo      VARCHAR(255),
    descripcion TEXT
);

CREATE TABLE usuario.requerimiento_no_funcional (
    id          BIGSERIAL PRIMARY KEY,
    proyecto_id BIGINT NOT NULL REFERENCES usuario.proyecto (id) ON DELETE CASCADE,
    codigo      VARCHAR(255),
    categoria   VARCHAR(255),
    descripcion TEXT
);

CREATE TABLE usuario.caso_uso (
    id          BIGSERIAL PRIMARY KEY,
    proyecto_id BIGINT NOT NULL REFERENCES usuario.proyecto (id) ON DELETE CASCADE,
    codigo      VARCHAR(255),
    nombre      VARCHAR(255),
    descripcion TEXT
);
