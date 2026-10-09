-- Схема базы данных для HireSystem (КР №1 и КР №2)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYER' CHECK (role IN ('ADMIN', 'EMPLOYER', 'CANDIDATE')),
    company_name VARCHAR(150),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vacancies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    salary_min NUMERIC(12, 2) CHECK (salary_min >= 0),
    salary_max NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED', 'REJECTED')),
    employer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_salary_range CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min)
);

CREATE INDEX IF NOT EXISTS idx_vacancies_employer_id ON vacancies(employer_id);
CREATE INDEX IF NOT EXISTS idx_vacancies_status ON vacancies(status);
CREATE INDEX IF NOT EXISTS idx_vacancies_title ON vacancies(title);

-- Тестовые данные для работодателей и вакансий
INSERT INTO users (email, password_hash, full_name, role, company_name) VALUES
('yandex_hr@yandex.ru', 'hash1', 'Анна Смирнова', 'EMPLOYER', 'Яндекс'),
('sber_hr@sber.ru', 'hash2', 'Иван Ковалев', 'EMPLOYER', 'СберТех'),
('tinkoff_hr@tbank.ru', 'hash3', 'Елена Романова', 'EMPLOYER', 'Т-Банк'),
('ozon_hr@ozon.ru', 'hash4', 'Дмитрий Волков', 'EMPLOYER', 'Озон'),
('admin@mirea.ru', 'adminhash', 'Главный Администратор', 'ADMIN', 'HR-Агентство МИРЭА')
ON CONFLICT (email) DO NOTHING;

INSERT INTO vacancies (title, description, salary_min, salary_max, status, employer_id) VALUES
('Java Backend Developer', 'Разработка микросервисов на Spring Boot и PostgreSQL', 160000, 240000, 'ACTIVE', 1),
('Senior Java / Kotlin Архитектор', 'Проектирование распределенных отказоустойчивых систем', 320000, 450000, 'ACTIVE', 2),
('Junior Java QA Automation', 'Написание автотестов JUnit 5, Selenide, CI/CD', 80000, 110000, 'ACTIVE', 3),
('Team Lead Java разработки', 'Управление командой из 6 разработчиков, Agile / Scrum', 350000, 480000, 'ARCHIVED', 1),
('Java Desktop Developer (JavaFX)', 'Поддержка и развитие десктопного инструментария', 140000, 200000, 'ACTIVE', 4),
('Middle Spring Boot Engineer', 'Интеграция с платежными шлюзами и брокерами Kafka', 190000, 260000, 'ACTIVE', 3),
('Стажер Java Developer', 'Участие в open-source проектах и обучение в команде', 45000, 65000, 'ACTIVE', 2),
('Legacy Java 8 Support Specialist', 'Рефакторинг старых монолитных систем', 120000, 150000, 'REJECTED', 4);
