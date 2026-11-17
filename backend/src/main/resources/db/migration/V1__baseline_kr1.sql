-- Baseline schema from KR1 (HireSystem)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYER',
    company_name VARCHAR(150),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vacancies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    salary_min NUMERIC(12, 2) CHECK (salary_min >= 0),
    salary_max NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    employer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_salary_range CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min)
);

CREATE INDEX IF NOT EXISTS idx_vacancies_employer_id ON vacancies(employer_id);
CREATE INDEX IF NOT EXISTS idx_vacancies_status ON vacancies(status);
CREATE INDEX IF NOT EXISTS idx_vacancies_title ON vacancies(title);

INSERT INTO users (email, password_hash, full_name, role, company_name) VALUES
('yandex_hr@yandex.ru', 'hash1', 'Анна Смирнова', 'EMPLOYER', 'Яндекс'),
('sber_hr@sber.ru', 'hash2', 'Иван Ковалев', 'EMPLOYER', 'СберТех'),
('tinkoff_hr@tbank.ru', 'hash3', 'Елена Романова', 'EMPLOYER', 'Т-Банк'),
('ozon_hr@ozon.ru', 'hash4', 'Дмитрий Волков', 'EMPLOYER', 'Озон'),
('admin@mirea.ru', 'adminhash', 'Главный Администратор', 'ADMIN', 'HR-Агентство МИРЭА')
ON CONFLICT (email) DO NOTHING;

INSERT INTO vacancies (title, description, salary_min, salary_max, status, employer_id)
SELECT v.title, v.description, v.salary_min, v.salary_max, v.status, u.id
FROM (VALUES
    ('Java Backend Developer', 'Разработка микросервисов на Spring Boot и PostgreSQL', 160000::numeric, 240000::numeric, 'ACTIVE', 'yandex_hr@yandex.ru'),
    ('Senior Java / Kotlin Архитектор', 'Проектирование распределенных отказоустойчивых систем', 320000::numeric, 450000::numeric, 'ACTIVE', 'sber_hr@sber.ru'),
    ('Junior Java QA Automation', 'Написание автотестов JUnit 5, Selenide, CI/CD', 80000::numeric, 110000::numeric, 'ACTIVE', 'tinkoff_hr@tbank.ru'),
    ('Team Lead Java разработки', 'Управление командой из 6 разработчиков, Agile / Scrum', 350000::numeric, 480000::numeric, 'ARCHIVED', 'yandex_hr@yandex.ru'),
    ('Java Desktop Developer (JavaFX)', 'Поддержка и развитие десктопного инструментария', 140000::numeric, 200000::numeric, 'ACTIVE', 'ozon_hr@ozon.ru'),
    ('Middle Spring Boot Engineer', 'Интеграция с платежными шлюзами и брокерами Kafka', 190000::numeric, 260000::numeric, 'ACTIVE', 'tinkoff_hr@tbank.ru'),
    ('Стажер Java Developer', 'Участие в open-source проектах и обучение в команде', 45000::numeric, 65000::numeric, 'ACTIVE', 'sber_hr@sber.ru'),
    ('Legacy Java 8 Support Specialist', 'Рефакторинг старых монолитных систем', 120000::numeric, 150000::numeric, 'REJECTED', 'ozon_hr@ozon.ru')
) AS v(title, description, salary_min, salary_max, status, email)
JOIN users u ON u.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM vacancies WHERE title = v.title AND employer_id = u.id);
