CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE employers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) UNIQUE,
    industry VARCHAR(100),
    website VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO employers (uuid, name, code)
SELECT gen_random_uuid(), company_name,
       UPPER(REGEXP_REPLACE(LEFT(company_name, 20), '[^A-Za-zА-Яа-я0-9]+', '_', 'g'))
FROM (
    SELECT DISTINCT company_name
    FROM users
    WHERE company_name IS NOT NULL AND btrim(company_name) <> ''
) src
ON CONFLICT DO NOTHING;

ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS employer_ref_id BIGINT;

UPDATE vacancies v
SET employer_ref_id = e.id
FROM users u
JOIN employers e ON e.name = u.company_name
WHERE v.employer_id = u.id AND v.employer_ref_id IS NULL;

-- Fallback: create generic employer for orphans
INSERT INTO employers (uuid, name, code)
SELECT gen_random_uuid(), 'Неизвестный работодатель', 'UNKNOWN'
WHERE EXISTS (SELECT 1 FROM vacancies WHERE employer_ref_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM employers WHERE code = 'UNKNOWN');

UPDATE vacancies SET employer_ref_id = (SELECT id FROM employers WHERE code = 'UNKNOWN' LIMIT 1)
WHERE employer_ref_id IS NULL;

ALTER TABLE vacancies DROP CONSTRAINT IF EXISTS vacancies_employer_id_fkey;
ALTER TABLE vacancies DROP COLUMN employer_id;
ALTER TABLE vacancies RENAME COLUMN employer_ref_id TO employer_id;
ALTER TABLE vacancies ALTER COLUMN employer_id SET NOT NULL;
ALTER TABLE vacancies
    ADD CONSTRAINT vacancies_employer_id_fkey
    FOREIGN KEY (employer_id) REFERENCES employers(id);

CREATE INDEX IF NOT EXISTS idx_employers_name ON employers(name);
CREATE INDEX IF NOT EXISTS idx_employers_enabled ON employers(enabled);
