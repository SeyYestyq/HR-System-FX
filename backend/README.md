# HR System Backend (КР №3)

Spring Boot REST API + PostgreSQL + Flyway + JWT + Swagger + Docker Compose + статический веб-интерфейс.

Тема: система найма (вакансии и работодатели). JavaFX-клиент из КР №2 не изменяется.

## Требования

- Docker Desktop
- JDK 21 (для локального `mvn test`)
- Maven 3.9+

## Быстрый запуск

```bash
cd backend
./start-compose.sh
```

Windows:

```powershell
.\start-compose.ps1
```

Адреса:

| Назначение | URL |
|---|---|
| Веб-интерфейс | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| REST API | http://localhost:8080/api |
| PostgreSQL | localhost:55432 / db `hr_system` |

Учётка администратора (из `.env`):

- email: `admin@example.org`
- пароль: `Adm1!ChangeMeNow`

После первого входа система потребует сменить временный пароль.

## Роли

- `VIEWER` — только чтение
- `EDITOR` — вакансии, заметки, массовки, CSV
- `ADMIN` — всё выше + работодатели, пользователи, аудит, permanent delete, Swagger

## Основные API

- `POST /api/auth/login|refresh|logout`, `GET /api/auth/me`, `POST /api/auth/change-password`
- `GET|POST|PUT|DELETE /api/vacancies`, restore/permanent/bulk/import/export
- `GET|POST|PUT /api/employers`
- `GET /api/analytics/summary`
- `GET|POST|PUT /api/admin/users`
- `GET /api/admin/audit`

## Тесты

```bash
mvn test
```

## Миграции Flyway

1. `V1` — baseline схемы КР1
2. `V2` — справочник работодателей
3. `V3` — поля вакансий КР3, теги, заметки
4. `V4` — security/audit (uuid пользователей, refresh-сессии, аудит)
