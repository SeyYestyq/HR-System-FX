# Эдик — ТЗ: миграция БД и креды подключения

> **Роль:** DevOps и Database Lead.
> **Закреплённые файлы:**
> - `javafx-client/migration_v2.sql`
> - `javafx-client/.env`, `javafx-client/docker-compose.yml`, `javafx-client/init.sql`
> - `src/main/resources/database.properties`
> - `src/main/java/ru/mirea/hrsystem/util/DatabaseManager.java`

Твои задачи: **T1** (миграция схемы) и **T2** (креды и fail-fast). Формат лёгкого ТЗ: суть-глагол, контекст одной строкой, требования списком, проверяемый DoD.

---

## Git-ветка

**Ветка:** `feature/db-migration`
**Базовая ветка:** `main`
**В этой ветке выполняются все задачи файла:** T1 и T2.

Порядок:
```bash
git switch main && git pull
git switch -c feature/db-migration
# коммиты по задачам: T1 → T2
git push -u origin feature/db-migration
# Pull Request в main → merge → удалить ветку
```

---

## T1. Привести схему БД к КР №1

**Исполнитель:** Эдик | **Дедлайн:** День 1, до 12:00 | **Ветка:** `feature/db-migration`

**Платформа (уже готова):** `javafx-client/migration_v2.sql` уже создан и идемпотентно добавляет `users.full_name`, `users.company_name`, `vacancies.description` через `ALTER TABLE ... IF NOT EXISTS` и бэкфилл. `javafx-client/docker-compose.yml` и `init.sql` уже поднимают базу КР1 с кредами `hr_user`/`hr_password`. `PostgresVacancyDao.findAll()` уже ждёт эти колонки.

**Тебе написать:**
1. Применить миграцию к поднятой базе КР1: `docker exec -i hr-system-postgres psql -U hr_user -d hr_system_db < migration_v2.sql`.
2. Проверить, что DAO работает: `\d users` показывает `full_name`, `company_name`; `\d vacancies` — `description`; `findAll()` возвращает строки.
3. Прогнать скрипт дважды — повторный запуск не должен падать.
4. В `03_DATABASE_ARCHITECTURE.md` описать миграцию вместо фразы «схема сохраняется из КР №1».

**Не трогаешь:** Flyway/Liquibase, автоматические миграции при старте.
**Артефакты:** `javafx-client/init.sql`, `javafx-client/docker-compose.yml` (креды `hr_user`/`hr_password`), `javafx-client/migration_v2.sql`.
**DoD:**
- [ ] Скрипт выполняется **дважды** без ошибок.
- [ ] `psql -U hr_user -d hr_system_db -c "\d users"` показывает `full_name`, `company_name`.
- [ ] `psql ... -c "\d vacancies"` показывает `description`.
- [ ] `PostgresVacancyDao.findAll()` возвращает строки, `SQLException` нет.

---

## T2. Вынести креды и включить fail-fast

**Исполнитель:** Эдик | **Дедлайн:** День 1, до 15:00 | **Ветка:** `feature/db-migration`

**Платформа (уже готова):** `DatabaseManager` уже читает `database.properties` и падает с `IllegalStateException("database.properties не найден или db.password не задан")`, если ресурса нет или пароль пуст. `database.properties` уже содержит `db.user=hr_user` / `db.password=hr_password`, а `.env` — `POSTGRES_USER=hr_user` / `POSTGRES_PASSWORD=hr_password`. `HrApplication.start` уже включает `InMemoryVacancyDao` только при `testConnection() == false`.

**Тебе написать:**
1. Проверить, что `database.properties` и `.env` используют одного пользователя `hr_user`.
2. Проверить, что в Java-коде не осталось пароля и фолбэка `postgres/postgres`.
3. Проверить, что без `database.properties` приложение падает с понятным текстом, а не с `null`.

**Не трогаешь:** пул соединений, шифрование конфига.
**Артефакты:** `src/main/resources/database.properties`, `util/DatabaseManager.java`, `HrApplication.java`, `javafx-client/.env`.
**DoD:**
- [ ] `grep -rn "postgres" src/main/java` не находит пароль.
- [ ] Без `database.properties` приложение падает с понятным текстом, не с `null`.
- [ ] С Docker в шапке «🟢 PostgreSQL подключен», без Docker — «🟠 Автономный режим (Stub)».
- [ ] `database.properties` и `.env` используют одного пользователя `hr_user`.

---

## Точки передачи кода

| Кто → кому | Что передаётся | Ссылка в коде |
|---|---|---|
| Эдик → Максим | схема после миграции | `migration_v2.sql` → запросы `PostgresVacancyDao`, `PostgresUserDao` |
| Эдик → всем | конфиг | `database.properties` → `DatabaseManager.getConnection()` |
| Эдик → Глебу | статус БД | `DatabaseManager.testConnection()` → `MainController.setDbStatus(boolean)` |
| Эдик → Дамиру | флаг stub | `HrApplication.start` выбирает `PostgresVacancyDao` или `InMemoryVacancyDao` |
