-- V2: Datos semilla del catálogo de habilidades (perfil de usuario).
-- Permite que el perfil de habilidades funcione sin cargar el catálogo manualmente.
-- (La siembra del catálogo de tecnologías se incorporará en el sprint de proyectos.)

INSERT INTO usuario.habilidad (nombre, area) VALUES
    ('Java', 'Backend'),
    ('Spring Boot', 'Backend'),
    ('Kotlin', 'Movil'),
    ('Jetpack Compose', 'Movil'),
    ('React', 'Frontend'),
    ('Django', 'Backend'),
    ('Python', 'Backend'),
    ('PostgreSQL', 'Base de datos'),
    ('Analisis', 'Gestion'),
    ('Requerimientos', 'Gestion'),
    ('Gestion de proyectos', 'Gestion'),
    ('Pruebas', 'Calidad')
ON CONFLICT (nombre) DO NOTHING;
