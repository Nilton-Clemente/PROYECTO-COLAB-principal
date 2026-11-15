-- V12: Bloqueo de cuentas de usuario desde la administración.
ALTER TABLE usuario.usuario ADD COLUMN bloqueada BOOLEAN NOT NULL DEFAULT FALSE;