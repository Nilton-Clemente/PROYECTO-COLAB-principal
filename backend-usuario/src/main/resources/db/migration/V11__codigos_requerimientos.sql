-- Asigna códigos deterministas a los registros anteriores, por proyecto y tipo.
WITH numerados AS (
    SELECT id, row_number() OVER (PARTITION BY proyecto_id ORDER BY id) AS numero
    FROM usuario.requerimiento_funcional
)
UPDATE usuario.requerimiento_funcional r
SET codigo = 'RF-' || lpad(n.numero::text, greatest(2, length(n.numero::text)), '0')
FROM numerados n WHERE r.id = n.id;

WITH numerados AS (
    SELECT id, row_number() OVER (PARTITION BY proyecto_id ORDER BY id) AS numero
    FROM usuario.requerimiento_no_funcional
)
UPDATE usuario.requerimiento_no_funcional r
SET codigo = 'RNF-' || lpad(n.numero::text, greatest(2, length(n.numero::text)), '0')
FROM numerados n WHERE r.id = n.id;

ALTER TABLE usuario.requerimiento_funcional ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE usuario.requerimiento_no_funcional ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE usuario.requerimiento_funcional ADD CONSTRAINT rf_codigo_proyecto_uniq UNIQUE (proyecto_id, codigo);
ALTER TABLE usuario.requerimiento_no_funcional ADD CONSTRAINT rnf_codigo_proyecto_uniq UNIQUE (proyecto_id, codigo);

ALTER TABLE usuario.proyecto ADD COLUMN siguiente_rf BIGINT NOT NULL DEFAULT 1;
ALTER TABLE usuario.proyecto ADD COLUMN siguiente_rnf BIGINT NOT NULL DEFAULT 1;
UPDATE usuario.proyecto p SET
    siguiente_rf = 1 + (SELECT count(*) FROM usuario.requerimiento_funcional r WHERE r.proyecto_id = p.id),
    siguiente_rnf = 1 + (SELECT count(*) FROM usuario.requerimiento_no_funcional r WHERE r.proyecto_id = p.id);