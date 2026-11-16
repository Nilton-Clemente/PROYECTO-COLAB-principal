-- Referencia opcional al icono local compartido entre los clientes.
ALTER TABLE usuario.habilidad ADD COLUMN icono VARCHAR(64);
ALTER TABLE usuario.tecnologia ADD COLUMN icono VARCHAR(64);