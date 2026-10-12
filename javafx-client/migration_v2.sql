-- ============================================================================
-- Миграция схемы БД КР №1 → требованиям КР №2
-- ----------------------------------------------------------------------------
-- Скрипт идемпотентен: его можно запускать повторно, ошибок он не даёт.
--
-- Запуск:
--   docker exec -i hr_system_postgres psql -U hr_user -d hr_system_db < migration_v2.sql
--
-- Что делает:
--   1. Добавляет недостающие колонки: users.full_name, users.company_name,
--      vacancies.description.
--   2. Бэкфиллет users.full_name из локальной части email.
--   3. Бэкфиллет users.company_name из vacancies.company_name — ТОЛЬКО если эта
--      колонка реально есть в базе (в части баз КР №1 её нет).
--   4. Приводит users.full_name к NOT NULL, чтобы мигрированная схема совпадала
--      с канонической схемой из init.sql.
-- ============================================================================

-- 1. Недостающие колонки ------------------------------------------------------------
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_name VARCHAR(150);
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS description TEXT;

-- 2. Бэкфилл users.full_name из локальной части email --------------------------------
UPDATE users
SET full_name = split_part(email, '@', 1)
WHERE full_name IS NULL OR btrim(full_name) = '';

-- 3. Бэкфилл users.company_name ------------------------------------------------------
-- Колонка vacancies.company_name есть не в каждой базе КР №1, поэтому обращаемся
-- к ней только после проверки information_schema. Иначе скрипт падал бы с
-- "column company_name does not exist".
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name   = 'vacancies'
          AND column_name  = 'company_name'
    ) THEN
        UPDATE users u
        SET company_name = src.company_name
        FROM (
            SELECT DISTINCT ON (employer_id) employer_id, company_name
            FROM vacancies
            WHERE company_name IS NOT NULL AND btrim(company_name) <> ''
            ORDER BY employer_id, id
        ) src
        WHERE u.id = src.employer_id
          AND (u.company_name IS NULL OR btrim(u.company_name) = '');

        RAISE NOTICE 'company_name: бэкфилл выполнен из vacancies.company_name';
    ELSE
        RAISE NOTICE 'company_name: в vacancies нет company_name, бэкфилл пропущен';
    END IF;
END
$$;

-- 4. users.full_name NOT NULL — как в канонической схеме init.sql ----------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name   = 'users'
          AND column_name  = 'full_name'
          AND is_nullable  = 'YES'
    ) THEN
        EXECUTE 'ALTER TABLE users ALTER COLUMN full_name SET NOT NULL';
        RAISE NOTICE 'full_name: установлен NOT NULL';
    END IF;
END
$$;