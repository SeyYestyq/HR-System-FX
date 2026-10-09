# Максим — ТЗ: JDBC DAO и безопасный экспорт

> **Роль:** Data Access Lead, чистый JDBC, подготовка снимка для экспорта.
> **Закреплённые файлы:**
> - `src/main/java/ru/mirea/hrsystem/repository/VacancyDao.java`
> - `src/main/java/ru/mirea/hrsystem/repository/UserDao.java`
> - `src/main/java/ru/mirea/hrsystem/repository/PostgresVacancyDao.java`
> - `src/main/java/ru/mirea/hrsystem/repository/PostgresUserDao.java`
> - `src/main/java/ru/mirea/hrsystem/service/CsvExportService.java`

Твои задачи: **T9** (сервис экспорта) и **сверка SQL DAO со схемой КР1 после миграции T1**. Формат лёгкого ТЗ: суть-глагол, контекст одной строкой, требования списком, проверяемый DoD.

> ⚠️ **Важно про схему.** DAO читает колонки `u.company_name`, `u.email`, `users.full_name`, `v.description`. В базе КР №1 их нет. Эти имена станут валидными только **после** применения `javafx-client/migration_v2.sql` (задача T1, Эдик). До миграции `findAll()` падает с `SQLException` и приложение уходит в stub.

---

## Git-ветка

**Ветка:** `feature/dao-export`
**Базовая ветка:** `main`
**В этой ветке выполняются все задачи файла:** T1-зависимость (сверка SQL DAO) и T9 (сервисная часть).

Порядок:
```bash
git switch main && git pull
git switch -c feature/dao-export
# коммиты: сверка DAO после T1 → T9
git push -u origin feature/dao-export
# Pull Request в main → merge → удалить ветку
```

---

## T1-зависимость. Сверить SQL DAO со схемой после миграции

**Исполнитель:** Максим | **Дедлайн:** День 1, до 17:00 | **Ветка:** `feature/dao-export`

**Платформа (уже готова):** `javafx-client/migration_v2.sql` уже создан: добавляет `users.full_name`, `users.company_name`, `vacancies.description` через `ALTER TABLE ... IF NOT EXISTS` и делает бэкфилл. `PostgresVacancyDao` и `PostgresUserDao` уже используют `PreparedStatement` и выбирают `u.company_name`, `u.email`, `v.description`, `users.full_name`. `DatabaseManager.getConnection()` уже отдаёт соединение.

**Тебе написать:**
1. Применить `migration_v2.sql` к поднятой базе КР1 и убедиться, что DAO работает.
2. Проверить `findAll()` в `PostgresVacancyDao` и `findAllEmployers()` в `PostgresUserDao` на реальной схеме.
3. Проверить, что остались только `PreparedStatement`: ни одного `Statement` со склейкой параметров.

**Не трогаешь:** Flyway/Liquibase, ORM, автоматические миграции при старте.
**Артефакты:** `javafx-client/migration_v2.sql`, `PostgresVacancyDao.findAll()`, `PostgresUserDao.findAllEmployers()`.
**DoD:**
- [ ] До миграции `findAll()` падает с `SQLException`; после миграции возвращает строки.
- [ ] `grep -n "u.company_name\|u.email\|v.description" PostgresVacancyDao.java` находит JOIN-выборку.
- [ ] `findAllEmployers()` читает `users.company_name` и `users.full_name`.
- [ ] Нет ни одного `Statement` со склейкой параметров — только `PreparedStatement`.

---

## T9. Отдавать снимок списка в экспорт

**Исполнитель:** Максим | **Дедлайн:** День 5, до 14:00 | **Ветка:** `feature/dao-export`

**Платформа (уже готова):** `CsvExportService.export(List<Vacancy>, File)` уже пишет файл в UTF-8 с BOM и не импортирует `javafx.*`. `MainController.handleExportCsv()` уже снимает снимок `new ArrayList<>(filteredData)`, дизейблит `exportButton` и запускает экспорт через `FxTasks.run`.

**Тебе написать:**
1. Проверить, что `CsvExportService.export(List<Vacancy>, File)` пишет только файл — без `javafx.*` и без `UiUtils`.
2. Проверить, что в `MainController.handleExportCsv()` экспорт получает снимок `new ArrayList<>(filteredData)`, а не живую `filteredData`.
3. Проверить кодировку UTF-8 с BOM и дизейбл `exportButton` на время выгрузки.

**Не трогаешь:** экспорт в `.xlsx`, стриминг на диск.
**Артефакты:** `service/CsvExportService.java`, `MainController.handleExportCsv()`, `ui/util/FxTasks.java`.
**DoD:**
- [ ] Во время экспорта можно менять фильтр и поиск — исключения нет.
- [ ] Кнопка экспорта заблокирована до завершения и разблокируется после.
- [ ] В файле ровно столько строк, сколько было показано на момент старта.
- [ ] `grep -rn "javafx" src/main/java/ru/mirea/hrsystem/service` — пусто.

---

## Точки передачи кода

| Кто → кому | Что передаётся | Ссылка в коде |
|---|---|---|
| Эдик → Максим | схема после миграции | `migration_v2.sql` → запросы `PostgresVacancyDao` / `PostgresUserDao` |
| Эдик → Максим | соединение | `DatabaseManager.getConnection()` |
| Максим → Глеб | чистый экспорт | `CsvExportService.export(List<Vacancy>, File)` вызывается из `MainController.handleExportCsv()` |
| Максим → Дамиру | контракт | `VacancyDao` (`findAll`, `findById`, `create`, `update`, `delete`), `UserDao.findAllEmployers()` |
