"""Iconos Devicon incluidos en backend-usuario; mantener sincronizados con sus SVG."""

ICONOS = (
    ('android', 'Android'),
    ('django', 'Django'),
    ('docker', 'Docker'),
    ('flutter', 'Flutter'),
    ('git', 'Git'),
    ('github', 'GitHub'),
    ('java', 'Java'),
    ('javascript', 'JavaScript'),
    ('kotlin', 'Kotlin'),
    ('mongodb', 'MongoDB'),
    ('mysql', 'MySQL'),
    ('nodejs', 'Node.js'),
    ('postgresql', 'PostgreSQL'),
    ('python', 'Python'),
    ('react', 'React'),
    ('spring', 'Spring'),
    ('typescript', 'TypeScript'),
)
IDENTIFICADORES_ICONOS = frozenset(identificador for identificador, _ in ICONOS)