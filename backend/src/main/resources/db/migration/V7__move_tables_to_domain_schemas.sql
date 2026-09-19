-- Organiza las tablas existentes por dominio (un schema por modulo del backend).
BEGIN;

CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS authentication;
CREATE SCHEMA IF NOT EXISTS profiles;

ALTER TABLE public.app_users SET SCHEMA users;
ALTER TABLE public.auth_sessions SET SCHEMA authentication;
ALTER TABLE public.developer_profiles SET SCHEMA profiles;

COMMIT;
