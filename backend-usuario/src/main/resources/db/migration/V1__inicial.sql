-- V1: Esquema base del área de Usuario (autenticación + perfil de habilidades)

CREATE SCHEMA IF NOT EXISTS usuario;

-- 15.1 Usuario
CREATE TABLE usuario.usuario (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    nombre          VARCHAR(255) NOT NULL,
    fecha_registro  TIMESTAMP NOT NULL DEFAULT now()
);

-- 15.2 Habilidad (catálogo)
CREATE TABLE usuario.habilidad (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE,
    area   VARCHAR(255)
);

-- 15.3 UsuarioHabilidad (N:M entre Usuario y Habilidad)
CREATE TABLE usuario.usuario_habilidad (
    usuario_id   BIGINT NOT NULL REFERENCES usuario.usuario (id) ON DELETE CASCADE,
    habilidad_id BIGINT NOT NULL REFERENCES usuario.habilidad (id) ON DELETE CASCADE,
    nivel        VARCHAR(50),
    PRIMARY KEY (usuario_id, habilidad_id)
);
