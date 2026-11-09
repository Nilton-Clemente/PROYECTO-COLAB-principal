-- V10: Especificación detallada opcional de los casos de uso.
-- Las filas existentes conservan sus datos y los campos nuevos quedan en NULL.
ALTER TABLE usuario.caso_uso
    ADD COLUMN actores TEXT,
    ADD COLUMN precondiciones TEXT,
    ADD COLUMN disparador TEXT,
    ADD COLUMN flujo_principal TEXT,
    ADD COLUMN flujos_alternativos TEXT,
    ADD COLUMN excepciones TEXT,
    ADD COLUMN postcondiciones TEXT,
    ADD COLUMN reglas_de_negocio TEXT;