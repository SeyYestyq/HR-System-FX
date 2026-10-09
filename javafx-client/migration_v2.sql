-- Миграция схемы КР №1 → требования КР №2 (идемпотентно, можно запускать повторно)
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_name VARCHAR(150);
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS description TEXT;

-- Бэкфилл: в КР1 у users нет full_name
UPDATE users SET full_name = split_part(email, '@', 1) WHERE full_name IS NULL OR full_name = '';
-- Бэкфилл company_name для работодателей из vacancies.company_name (если там есть)
UPDATE users u
SET company_name = src.company_name
FROM (SELECT DISTINCT employer_id, company_name FROM vacancies WHERE company_name IS NOT NULL) src
WHERE u.id = src.employer_id AND (u.company_name IS NULL OR u.company_name = '');
