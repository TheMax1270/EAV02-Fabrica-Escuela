-- Promueve una cuenta existente al rol ADMIN.

-- PASO 1: reemplazar el username y revisar que sea la cuenta correcta
SELECT id, full_name, username, email, role, enabled, created_at
FROM users.app_users
WHERE lower(username) = lower('REEMPLAZAR_USERNAME');

-- PASO 2:
UPDATE users.app_users
SET role = 'ADMIN'
WHERE lower(username) = lower('REEMPLAZAR_USERNAME')
  AND role = 'DEVELOPER';

