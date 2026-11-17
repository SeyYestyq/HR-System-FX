ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS uuid UUID UNIQUE DEFAULT gen_random_uuid();
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS code VARCHAR(30) UNIQUE;
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS is_remote BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE vacancies SET uuid = gen_random_uuid() WHERE uuid IS NULL;
ALTER TABLE vacancies ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE vacancies ALTER COLUMN uuid SET DEFAULT gen_random_uuid();

-- Migrate KR1 statuses
UPDATE vacancies SET status = 'OPEN', archived_at = NULL WHERE status = 'ACTIVE';
UPDATE vacancies SET status = 'CLOSED', archived_at = COALESCE(archived_at, CURRENT_TIMESTAMP) WHERE status = 'ARCHIVED';
UPDATE vacancies SET status = 'CLOSED', archived_at = COALESCE(archived_at, CURRENT_TIMESTAMP) WHERE status = 'REJECTED';

-- Assign codes
WITH numbered AS (
    SELECT id, 'VAC-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(ROW_NUMBER() OVER (ORDER BY id)::text, 4, '0') AS new_code
    FROM vacancies
    WHERE code IS NULL
)
UPDATE vacancies v SET code = n.new_code FROM numbered n WHERE v.id = n.id;
ALTER TABLE vacancies ALTER COLUMN code SET NOT NULL;

ALTER TABLE vacancies DROP CONSTRAINT IF EXISTS vacancies_status_check;
ALTER TABLE vacancies ADD CONSTRAINT vacancies_status_check
    CHECK (status IN ('DRAFT', 'OPEN', 'PAUSED', 'CLOSED'));

CREATE TABLE IF NOT EXISTS vacancy_tags (
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (vacancy_id, tag)
);
CREATE INDEX IF NOT EXISTS idx_vacancy_tag_value ON vacancy_tags(tag);

CREATE TABLE IF NOT EXISTS vacancy_notes (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL,
    content VARCHAR(4000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_vacancy_notes_vacancy ON vacancy_notes(vacancy_id, pinned DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vacancies_uuid ON vacancies(uuid);
CREATE INDEX IF NOT EXISTS idx_vacancies_code ON vacancies(code);
CREATE INDEX IF NOT EXISTS idx_vacancies_is_remote ON vacancies(is_remote);
CREATE INDEX IF NOT EXISTS idx_vacancies_archived_at ON vacancies(archived_at);
CREATE INDEX IF NOT EXISTS idx_vacancies_salary_min ON vacancies(salary_min);
CREATE INDEX IF NOT EXISTS idx_vacancies_created_at ON vacancies(created_at);
