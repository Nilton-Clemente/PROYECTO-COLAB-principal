"""Iconos Devicon incluidos en backend-usuario; mantener sincronizados con sus SVG."""

ICONOS = (
    ('java', 'Java'),
    ('kotlin', 'Kotlin'),
    ('postgresql', 'PostgreSQL'),
    ('react', 'React'),
    ('spring', 'Spring'),
)
IDENTIFICADORES_ICONOS = frozenset(identificador for identificador, _ in ICONOS)