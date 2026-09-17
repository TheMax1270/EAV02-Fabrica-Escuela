-- PASO 1: revisar usuarios
SELECT id, full_name, username, email, created_at
FROM app_users
WHERE (
    left(lower(username), 5) = 'test_'
    OR left(lower(username), 5) = 'hu01_'
    OR left(lower(username), 5) = 'hu02_'
)
AND right(lower(email), 10) = '@eav02.com'
ORDER BY username;

-- PASO 2: ejecutar solo después de revisar el paso 1
/*
BEGIN;

CREATE TEMP TABLE devconnect_test_user_ids ON COMMIT DROP AS
SELECT id
FROM app_users
WHERE (
    left(lower(username), 5) = 'test_'
    OR left(lower(username), 5) = 'hu01_'
    OR left(lower(username), 5) = 'hu02_'
)
AND right(lower(email), 10) = '@eav02.com';

DO $$
BEGIN
    IF (SELECT count(*) FROM devconnect_test_user_ids) > 20 THEN
        RAISE EXCEPTION 'Too many test users selected; review before deleting';
    END IF;
    IF (SELECT count(*) FROM devconnect_test_user_ids) >= (SELECT count(*) FROM app_users) THEN
        RAISE EXCEPTION 'Refusing to delete every user';
    END IF;
END
$$;

DELETE FROM auth_sessions
WHERE user_id IN (SELECT id FROM devconnect_test_user_ids);

DELETE FROM app_users
WHERE id IN (SELECT id FROM devconnect_test_user_ids);

COMMIT;
*/
