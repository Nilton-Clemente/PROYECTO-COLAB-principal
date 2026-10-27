-- V5: Datos semilla de tecnologias
-- Permite registrar tecnologias en los proyectos sin cargarlas manualmente.

INSERT INTO usuario.tecnologia (nombre) VALUES
    ('Java'),
    ('Spring Boot'),
    ('Kotlin'),
    ('Jetpack Compose'),
    ('React'),
    ('Django'),
    ('Python'),
    ('PostgreSQL'),
    ('Flutter'),
    ('Node.js')
ON CONFLICT (nombre) DO NOTHING;
