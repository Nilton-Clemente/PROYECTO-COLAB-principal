-- Contexto del proyecto al generar el aviso; no depende de su existencia posterior.
ALTER TABLE usuario.notificacion
    ADD COLUMN proyecto_id BIGINT,
    ADD COLUMN proyecto_nombre VARCHAR(255);

-- Los avisos históricos no guardaron esta relación. No se infiere desde el mensaje.