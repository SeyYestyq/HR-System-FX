# HireSystem Desktop Client (Контрольная работа №2)

> **Настольное JavaFX-приложение для управления вакансиями и подбора специалистов**
> **Предметная область:** HR-агентство (продолжение КР №1)
> **Стек:** Java 21, OpenJFX 21 (`javafx-controls`, `javafx-fxml`), Maven, PostgreSQL 15 (Docker), чистый JDBC, JUnit 5, AssertJ, SLF4J.

---

## Навигация по документации

| Документ | Содержание | Ответственный |
|---|---|---|
| [01_SYSTEM_ARCHITECTURE.md](01_SYSTEM_ARCHITECTURE.md) | Архитектура, C4-диаграммы, слои (FXML → Controller → Service → DAO), Walking Skeleton | Вся команда |
| [02_FUNCTIONAL_SPECIFICATION.md](02_FUNCTIONAL_SPECIFICATION.md) | Экраны, сценарии CRUD, валидация, 5 метрик статистики, экспорт CSV | Вся команда |
| [03_DATABASE_ARCHITECTURE.md](03_DATABASE_ARCHITECTURE.md) | DDL, связи, тестовые данные, `database.properties`, параметризованные SQL | Эдик, Максим |
| [04_TEAM_WORK_PLAN.md](04_TEAM_WORK_PLAN.md) | 7-дневный спринт, микро-дедлайны, DoD дней | Дамир |
| [PROJECT_RULES.md](PROJECT_RULES.md) | Стандарты кода, Clean Architecture, потокобезопасность UI | Дамир |

Персональные ТЗ (задачи T1–T10 в формате «лёгкого ТЗ»):

1. **Дамир** — [DAMIR_VALIDATION_SERVICE_PLAN.md](DAMIR_VALIDATION_SERVICE_PLAN.md): сервисы T5, статистика T7, `VacancyDialog.fxml` T8, документы T10.
2. **Глеб** — [GLEB_JAVAFX_UI_PLAN.md](GLEB_JAVAFX_UI_PLAN.md): второй фильтр T3, `FxTasks` T4, окно статистики T6, UI-хук экспорта T9.
3. **Максим** — [MAXIM_JDBC_DAO_PLAN.md](MAXIM_JDBC_DAO_PLAN.md): снимок для экспорта T9, сверка SQL DAO со схемой после T1.
4. **Эдик** — [EDIK_DATABASE_STUB_PLAN.md](EDIK_DATABASE_STUB_PLAN.md): миграция T1, креды и fail-fast T2.

---

## Быстрый старт

### 1. Запуск PostgreSQL 15 в Docker

```bash
cd javafx-client
docker compose up -d
```

### 2. Запуск приложения

**macOS / Linux:**
```bash
cd javafx-client
./run.sh
# или
mvn javafx:run
```

**Windows (PowerShell):**
```powershell
cd javafx-client
.\run.ps1
# или
mvn javafx:run
```

Без Docker приложение стартует в автономном режиме: `InMemoryVacancyDao` и 8 тестовых вакансий.

### 3. Тесты

```bash
cd javafx-client
mvn clean test
```

---

## Миграция БД из КР №1

База КР №1 не содержит `users.full_name`, `users.company_name` и `vacancies.description`, поэтому её нужно доработать скриптом `javafx-client/migration_v2.sql`.

Применить миграцию к запущенному контейнеру:

```bash
cd javafx-client
docker exec -i hr-system-postgres psql -U hr_user -d hr_system_db < migration_v2.sql
```

Креды приложения (`src/main/resources/database.properties`) и контейнера (`javafx-client/.env`):

- пользователь — `hr_user`
- пароль — `hr_password`
- база — `hr_system_db`
- порт — `5432`

Скрипт идемпотентный (`ADD COLUMN IF NOT EXISTS`) — его можно выполнять повторно.
