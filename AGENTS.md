# AGENTS.md — Инструкция для ИИ-ассистентов (Codex, Cursor, Claude)

> **Действует для всех ИИ-сессий в проекте `2_kr`.** Читается автоматически перед началом любой работы.  
> Базовые стандарты архитектуры зафиксированы в [`PROJECT_RULES.md`](PROJECT_RULES.md) и [`01_SYSTEM_ARCHITECTURE.md`](01_SYSTEM_ARCHITECTURE.md).

---

## 1. Главные правила для ИИ при генерации кода

1. **Соблюдать слои (Clean Architecture):**
   - Контроллеры JavaFX (`ru.mirea.hrsystem.ui.*`) не содержат SQL-запросов и бизнес-логики.
   - Сервисы (`ru.mirea.hrsystem.service.*`) не содержат импортов `javafx.*`.
   - Доступ к базе только через интерфейсы DAO (`ru.mirea.hrsystem.repository.*`).
2. **Не блокировать UI-поток:**
   - Все долгие вызовы (БД, экспорт) оборачивать в фоновые задачи (`Task<T>`, `CompletableFuture`), а обновления интерфейса — строго в `Platform.runLater()`.
3. **Безопасный JDBC:**
   - Использовать только `PreparedStatement`. Никакой склейки SQL строк.
   - Всегда оборачивать `Connection`, `PreparedStatement`, `ResultSet` в `try-with-resources`.
4. **Никакого мусора и заглушек:**
   - Не использовать `System.out.println` — только SLF4J `logger`.
   - Не хардкодить пароли и пути — брать из `database.properties`.
5. **Верификация изменений:**
   - После изменения логики проверять запуск тестов командой `mvn test`.
