ALTER TABLE usuario.planificacion
    ADD COLUMN fecha_finalizacion TIMESTAMP,
    ADD COLUMN fecha_cancelacion TIMESTAMP;

-- La fecha real de cierre no está disponible para los proyectos históricos.
UPDATE usuario.planificacion p
SET estado = 'FINALIZADA'
FROM (
    SELECT e.planificacion_id
    FROM usuario.etapa e
    JOIN usuario.tarea t ON t.etapa_id = e.id
    GROUP BY e.planificacion_id
    HAVING bool_and(t.estado = 'COMPLETADA')
) completadas
WHERE p.id = completadas.planificacion_id AND p.estado = 'ACTIVA';