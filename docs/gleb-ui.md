# Интерфейс вакансий

Это описание окна, которое собрал Глеб. Оно показывает список вакансий, фильтрует его на экране и не замирает, пока данные читаются или пишутся.

Рядом лежат два соседних текста:

- [куда это подключать](gleb-integration.md) — кто какой метод вызывает и на какой ветке это лежит;
- [обучалка по Java](gleb-java-tutorial.md) — если код хочется разобрать с нуля.

Запуск из папки `javafx-client`: `mvn javafx:run`.

Если PostgreSQL на `localhost:5432` отвечает, шапка пишет «PostgreSQL подключен», строки читаются из базы. Если база выключена, шапка пишет «Автономный режим (Stub)», в таблице 8 учебных вакансий из памяти. Если файла `database.properties` нет совсем, приложение останавливается с понятной ошибкой, а не с пустым указателем.

## Что сделано и где это лежит

```mermaid
flowchart LR
    glebBranch[Gleb_dev_main_branch]
    mainBranch[main]
    testBranch[test/full-integration]
    glebBranch -->|фильтры фон окно статистики| testBranch
    mainBranch -->|расчёт пяти метрик| testBranch
```

На `Gleb_dev_main_branch` залит интерфейс: фильтры, фоновые задачи, окно статистики, кнопка экспорта и первая версия документов. На `main` был один чужой коммит — расчёт метрик в `StatisticsService`. Других веток на GitHub не было: ни DAO, ни CSV, ни формы, ни живого подключения к базе.

`test/full-integration` собирает оба коммита и дописывает то, без чего кнопки оставались мёртвыми:

| Кусок | Файл | Зачем |
|---|---|---|
| Подключение | `DatabaseManager` | настоящая проверка базы и запасной стаб |
| SQL вакансий | `PostgresVacancyDao` | чтение, создание, правка, удаление |
| Пять чисел | `StatisticsService` | пришло с `main` |
| CSV | `CsvExportService` | файл с BOM и кавычками |
| Форма | `VacancyDialog` и контроллер | создание и правка с проверкой полей |

## Из чего состоит окно

```mermaid
flowchart TB
    subgraph window [Главное окно]
        header[Шапка: бренд и статус базы]
        actions[Кнопки: создать, править, удалить, статистика, экспорт]
        filters[Поиск, статус, зарплата, сброс]
        table[Таблица вакансий]
        status[Строка: показано X из Y]
    end
    header --> actions --> filters --> table --> status
```

Разметка уже лежит в `javafx-client/src/main/resources/ru/mirea/hrsystem/view/MainView.fxml`. Java-код только наполняет её данными и реагирует на кнопки. Цвета и карточки статистики — в `css/app.css`.

| Файл | Зачем |
|---|---|
| `ui/MainController.java` | Таблица, фильтры, кнопки |
| `ui/util/FxTasks.java` | Уводит долгую работу с экрана |
| `ui/StatisticsDialog.java` | Пять карточек метрик |
| `ui/util/UiUtils.java` | Окна ошибок, успеха и подтверждения |
| `HrApplication.java` | Создаёт сервисы и отдаёт их контроллеру |

Контроллер не пишет SQL и не считает среднюю зарплату. Он просит сервис и рисует ответ.

```mermaid
flowchart LR
    fxml[MainView.fxml]
    controller[MainController]
    tasks[FxTasks]
    vacancyService[VacancyService]
    userDao[UserDao]
    stats[StatisticsService]
    csv[CsvExportService]
    dialog[StatisticsDialog]
    fxml --> controller
    controller --> tasks
    tasks --> vacancyService
    tasks --> userDao
    tasks --> stats
    tasks --> csv
    controller --> dialog
```

## Как окно появляется

`FXMLLoader` сначала создаёт контроллер и вызывает `initialize()`. В этот момент сервисов ещё нет: можно настроить колонки и фильтры, нельзя ходить в базу.

Потом `HrApplication` вызывает `initDependencies(...)` и `setDbStatus(...)`. Только здесь стартует загрузка.

```mermaid
sequenceDiagram
    participant App as HrApplication
    participant Fxml as FXMLLoader
    participant Ui as MainController
    participant Bg as FxTasks
    participant Svc as VacancyService

    App->>Fxml: load MainView.fxml
    Fxml->>Ui: initialize
    Note over Ui: колонки, фильтры, кнопки
    App->>Ui: initDependencies
    Ui->>Bg: run загрузка
    App->>Ui: setDbStatus
    App->>App: stage.show
    Bg->>Svc: getAllVacancies и findAllEmployers
    Svc-->>Bg: списки
    Bg->>Ui: onSuccess на UI-потоке
    Note over Ui: таблица заполнена, кнопки снова живые
```

Шапка зависит от флага, который пришёл снаружи:

- `true` — «PostgreSQL подключен»;
- `false` — «Автономный режим (Stub)».

`DatabaseManager.testConnection()` читает `database.properties` и пробует соединение. Успех — `true`. База не отвечает — `false` и стаб. Файла настроек нет — исключение `DatabaseException`, старт прерывается.

## Три списка, одна таблица

В памяти живут все загруженные вакансии. На экране виден не этот список, а два вида поверх него.

```mermaid
flowchart LR
    master[masterData все строки]
    filtered[filteredData поиск и два фильтра]
    sorted[SortedList клик по заголовку колонки]
    table[TableView]
    master --> filtered --> sorted --> table
```

Поиск не ходит в базу. Человек печатает «Spring», предикат пересчитывается, таблица сразу показывает подходящие строки. Счётчик внизу: «Показано: X из Y вакансий». X — сколько прошло фильтр, Y — сколько пришло с сервера.

Клик по заголовку колонки сортирует уже отфильтрованные строки. Исходный порядок в `masterData` при этом не ломается.

## Фильтры

Вакансия остаётся на экране, только если прошла все три проверки сразу.

```mermaid
flowchart TD
    row[Вакансия]
    text{Текст найден?}
    status{Статус подходит?}
    salary{Зарплата в диапазоне?}
    show[Строка видна]
    hide[Строка скрыта]
    row --> text
    text -->|нет| hide
    text -->|да| status
    status -->|нет| hide
    status -->|да| salary
    salary -->|нет| hide
    salary -->|да| show
```

Поиск без учёта регистра смотрит в название, компанию и описание. Пустое описание не роняет программу: перед `contains` есть проверка на `null`. Пустая строка поиска пропускает все вакансии.

Статусы в списке: «Все статусы», `ACTIVE`, `ARCHIVED`, `REJECTED`.

Зарплата сравнивается по полю «от» (`salaryMin`), не по верхней границе. Деньги — это `BigDecimal`, сравнение через `compareTo`, не через `==`.

| Пункт списка | Условие |
|---|---|
| Вся зарплата | любая, в том числе пустая |
| до 100 000 | `salaryMin` не пустая и `<= 100000` |
| 100 000–200 000 | от 100000 до 200000 включительно |
| от 200 000 | `salaryMin >= 200000` |

Если «от» не заполнено, узкий диапазон такую вакансию прячет.

На восьми учебных строках это выглядит так:

| Вакансия | От | Статус | до 100 тыс. | 100–200 | от 200 | слово Spring |
|---|---:|---|---|---|---|---|
| Java Backend Developer | 160000 | ACTIVE | | да | | да, в описании |
| Senior Java / Kotlin | 320000 | ACTIVE | | | да | |
| Junior Java QA | 80000 | ACTIVE | да | | | |
| Team Lead Java | 350000 | ARCHIVED | | | да | |
| Java Desktop Developer | 140000 | ACTIVE | | да | | |
| Middle Spring Boot | 190000 | ACTIVE | | да | | да, в названии |
| Стажер Java | 45000 | ACTIVE | да | | | |
| Legacy Java 8 | 120000 | REJECTED | | да | | |

«Сброс» очищает поле поиска и возвращает оба списка на первый пункт. Предикат пересчитается сам, отдельную кнопку «применить» нажимать не нужно.

Строка со статусом `ARCHIVED` рисуется полупрозрачной. Это псевдокласс `archived` на строке таблицы и правило `.table-row-cell:archived` в CSS. Цвет не зашит в Java через `setStyle`.

## Почему окно можно двигать во время загрузки

Чтение списка, удаление, расчёт статистики и запись CSV идут не в потоке окна. `FxTasks.run` создаёт `Task`, запускает его в daemon-потоке `fx-background` и возвращает результат обратно на поток JavaFX.

```mermaid
sequenceDiagram
    participant User as Пользователь
    participant Ui as Поток окна
    participant Task as fx-background
    participant Svc as Сервис

    User->>Ui: нажал Обновить
    Ui->>Ui: busy = true, кнопки серые
    Ui->>Task: call
    Note over Ui: окно двигается
    Task->>Svc: долгий вызов
    Svc-->>Task: список или ошибка
    Task->>Ui: onSuccess или onFailure
    Ui->>Ui: таблица обновлена, busy = false
```

В `onSuccess` можно менять таблицу. В `call()` — нельзя: JavaFX выбросит ошибку, если трогать кнопки из чужого потока.

Пока `busy` равен `true`, неактивны таблица, «Обновить», «Новая вакансия», «Редактировать», «Удалить» и «Экспорт». «Редактировать» и «Удалить» ещё и ждут выбранную строку. Двойной щелчок по строке открывает правку, но только когда окно не занято.

Экспорт устроен чуть иначе. На время записи гаснет только кнопка экспорта. Поиск можно менять: в файл уже ушёл снимок, живой список ему не нужен.

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Loading: Обновить, удалить, статистика, диалог
    Loading --> Idle: успех или ошибка
    Idle --> Exporting: файл выбран
    Exporting --> Idle: файл записан или ошибка
    Loading --> Loading: экспорт недоступен
```

## Что делает каждая кнопка

### Обновить

Снова читает вакансии и работодателей. Старые строки заменяются целиком через `masterData.setAll`.

### Новая вакансия и Редактировать

Сначала в фоне запрашивается список работодателей. Потом на потоке окна вызывается `VacancyDialog.show(owner, vacancy, employers)`. Для новой вакансии `vacancy` равен `null`. Для правки туда уходит выбранная строка. Метод возвращает заполненную вакансию или `null`, если нажали «Отмена».

Форма — `VacancyDialog.fxml`. Пустое или короткое название, пустая компания, буквы вместо зарплаты и «до» меньше «от» не закрывают окно: поля получают класс `field-error`, подпись объясняет, что проверить. «Сохранить» при верных данных возвращает объект. Контроллер в фоне вызывает `createVacancy` или `updateVacancy` и заново читает список.

```mermaid
sequenceDiagram
    participant Ui as MainController
    participant Form as VacancyDialog
    participant Svc as VacancyService
    Ui->>Form: show
    alt Отмена
        Form-->>Ui: null
    else Сохранить
        Form-->>Ui: Vacancy
        Ui->>Svc: create или update
        Ui->>Ui: loadData
    end
```

### Удалить

Спрашивает подтверждение. «Нет» ничего не делает. «Да» вызывает `vacancyService.deleteVacancy(id)` в фоне и заново грузит таблицу. На стабе удаление уже работает.

### Статистика

В фоне вызывается `statisticsService.calculateMetrics()`. Если метод вернул `StatisticsDto`, открывается модальное окно. Если бросил исключение, видно «Ошибка статистики», карточки с нулями не показываются.

Расчёт уже есть: все вакансии из того же DAO, что и таблица. На восьми учебных строках карточки показывают 8, 6 активных, 1 в архиве, среднюю «от» 175 625 ₽ и максимальную «до» 480 000 ₽. Пустой список даёт нули, деления на ноль нет.

Окно статистики — отдельный `Stage` поверх главного. Пока оно открыто, главное окно не кликается (`APPLICATION_MODAL`). Пять карточек:

| Карточка | Поле `StatisticsDto` |
|---|---|
| Всего вакансий | `totalVacancies` |
| Активных вакансий | `activeVacancies` |
| В архиве | `archivedVacancies` |
| Средняя зарплата от | `averageSalaryMin`, формат `175 625 ₽` |
| Максимальная зарплата | `maxSalary` |

Стили карточек: `metric-card`, `metric-value`, `metric-title`. Окно забирает ту же таблицу стилей, что и главное, поэтому карточки выглядят так же, как задумано в `app.css`. Кнопка «Закрыть» прячет окно.

```mermaid
flowchart TB
    owner[Главное окно]
    stage[Stage Статистика]
    scene[Scene]
    root[VBox]
    grid[GridPane 5 карточек]
    close[Кнопка Закрыть]
    owner -->|initOwner и стили| stage
    stage --> scene --> root
    root --> grid
    root --> close
```

Числа считает Дамир. Глеб только раскладывает готовый объект по подписям.

### Экспорт в CSV

На потоке окна открывается выбор файла. Если человек нажал «Отмена», ничего не пишется. Если файл выбран, текущий отфильтрованный список копируется в новый `ArrayList`. Эта копия уходит в `csvExportService.export(snapshot, file)`.

```mermaid
sequenceDiagram
    participant Ui as Поток окна
    participant Disk as Фоновый поток
    participant Csv as CsvExportService

    Ui->>Ui: FileChooser
    Ui->>Ui: snapshot = копия filteredData
    Ui->>Ui: кнопка экспорта погашена
    Ui->>Disk: export snapshot
    Note over Ui: фильтр можно менять
    Disk->>Csv: запись файла
    Disk->>Ui: успех с путём или ошибка экспорта
    Ui->>Ui: кнопка снова активна
```

Копия нужна, потому что человек может менять поиск, пока файл пишется. Если отдать в экспорт живой список, итерация и изменение списка столкнутся.

`CsvExportService` пишет UTF-8 с меткой BOM, разделитель `;`, заголовок `ID;Название вакансии;Компания;Зарплата от;Зарплата до;Статус;Описание`. Если в тексте есть точка с запятой, кавычки или перевод строки, поле берётся в кавычки, чтобы Excel не разъехался по колонкам. Ошибка записи по-прежнему показывает «Ошибка экспорта» и снова включает кнопку.

## Ошибки

Любой сбой фона попадает в `showError`: строка в лог SLF4J и окно `UiUtils.showErrorAlert`. Пустой `catch` нет. Сообщение берётся из исключения; если текста нет, показывается сам объект ошибки.

Окно при этом не закрывается.

## Откуда берётся список

```mermaid
flowchart TD
    props[database.properties]
    test{Соединение живо?}
    pg[PostgresVacancyDao]
    mem[InMemoryVacancyDao]
    svc[VacancyService и StatisticsService]
    props --> test
    test -->|да| pg --> svc
    test -->|нет| mem --> svc
```

`PostgresVacancyDao` читает вакансии вместе с названием компании (`LEFT JOIN users`), создаёт, обновляет и удаляет через `PreparedStatement`. Параметры передаются знаками `?`, не склейкой строки. Соединение, запрос и результат закрываются в `try-with-resources`.

Один и тот же DAO попадает и в таблицу, и в статистику. Поэтому цифры на карточках совпадают с тем, что лежит в базе или в стабе.

## Чего здесь нет

Графиков, фильтра по дате и выгрузки в Excel нет. Это не входило в задачи окна.

Проверки без окна: `VacancyFilterTest` (зарплата и «Spring»), `StatisticsServiceTest` (8 / 6 / 1 / 175 625 / 480 000 и нули на пустом списке), `CsvExportServiceTest` (кавычки вокруг точки с запятой), `VacancyServiceTest` (создание и архив). Вместе 13 тестов.
