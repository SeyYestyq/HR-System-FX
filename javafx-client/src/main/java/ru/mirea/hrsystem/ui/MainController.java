package ru.mirea.hrsystem.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.repository.UserDao;
import ru.mirea.hrsystem.service.CsvExportService;
import ru.mirea.hrsystem.service.StatisticsService;
import ru.mirea.hrsystem.service.VacancyService;
import ru.mirea.hrsystem.ui.util.FxTasks;
import ru.mirea.hrsystem.ui.util.UiUtils;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Главный FXML-контроллер настольного интерфейса HireSystem.
 * Отвечает за реактивную привязку элементов к модели данных,
 * живую фильтрацию, сортировку и асинхронное выполнение операций.
 */
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private static final PseudoClass ARCHIVED_PSEUDO_CLASS = PseudoClass.getPseudoClass("archived");

    private static final DecimalFormat MONEY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
        symbols.setGroupingSeparator(' ');
        MONEY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    // UI элементы из MainView.fxml
    @FXML private Label dbStatusLabel;
    @FXML private Button refreshButton;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button exportButton;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> salaryRangeFilterComboBox;

    @FXML private TableView<Vacancy> vacancyTableView;
    @FXML private TableColumn<Vacancy, Long> idColumn;
    @FXML private TableColumn<Vacancy, String> titleColumn;
    @FXML private TableColumn<Vacancy, String> employerColumn;
    @FXML private TableColumn<Vacancy, BigDecimal> salaryMinColumn;
    @FXML private TableColumn<Vacancy, BigDecimal> salaryMaxColumn;
    @FXML private TableColumn<Vacancy, String> statusColumn;

    @FXML private Label countLabel;

    // Реактивные коллекции
    private final ObservableList<Vacancy> masterData = FXCollections.observableArrayList();
    private FilteredList<Vacancy> filteredData;

    // Сервисные зависимости
    private VacancyService vacancyService;
    private UserDao userDao;
    private StatisticsService statisticsService;
    private CsvExportService csvExportService;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        setupTableSelection();
        setupTableRowFactory();
    }

    /**
     * Внедрение зависимостей сервисного слоя при старте приложения.
     */
    public void initDependencies(VacancyService vacancyService, UserDao userDao,
                                 StatisticsService statisticsService, CsvExportService csvExportService) {
        this.vacancyService = vacancyService;
        this.userDao = userDao;
        this.statisticsService = statisticsService;
        this.csvExportService = csvExportService;

        loadDataFromService();
    }

    /**
     * Отображает актуальный статус подключения к БД в верхней панели.
     */
    public void setDbStatus(boolean dbAvailable) {
        if (dbStatusLabel != null) {
            dbStatusLabel.getStyleClass().removeAll("header-status", "header-status-stub");
            if (dbAvailable) {
                dbStatusLabel.setText("🟢 PostgreSQL подключен");
                dbStatusLabel.getStyleClass().add("header-status");
            } else {
                dbStatusLabel.setText("🟠 Автономный режим (Stub)");
                dbStatusLabel.getStyleClass().add("header-status-stub");
            }
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        employerColumn.setCellValueFactory(data -> {
            Vacancy v = data.getValue();
            String name = v.getEmployerCompanyName();
            if (name == null || name.isBlank()) {
                name = v.getEmployerId() != null ? "ID #" + v.getEmployerId() : "—";
            }
            return new SimpleStringProperty(name);
        });

        salaryMinColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getSalaryMin()));
        salaryMinColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "—" : MONEY_FORMAT.format(item) + " ₽");
            }
        });

        salaryMaxColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getSalaryMax()));
        salaryMaxColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "—" : MONEY_FORMAT.format(item) + " ₽");
            }
        });

        statusColumn.setCellValueFactory(data -> {
            VacancyStatus status = data.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.name() : "");
        });

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Vacancy> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(vacancyTableView.comparatorProperty());
        vacancyTableView.setItems(sortedData);
    }

    private void setupFilters() {
        statusFilterComboBox.getItems().setAll("Все статусы", "ACTIVE", "ARCHIVED", "REJECTED");
        statusFilterComboBox.setValue("Все статусы");

        salaryRangeFilterComboBox.getItems().setAll("Вся зарплата", "до 100 000", "100 000–200 000", "от 200 000");
        salaryRangeFilterComboBox.setValue("Вся зарплата");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        salaryRangeFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
    }

    private void setupTableSelection() {
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        vacancyTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
        });
    }

    private void setupTableRowFactory() {
        vacancyTableView.setRowFactory(tv -> {
            TableRow<Vacancy> row = new TableRow<>() {
                @Override
                protected void updateItem(Vacancy vacancy, boolean empty) {
                    super.updateItem(vacancy, empty);
                    boolean isArchived = !empty && vacancy != null && vacancy.getStatus() == VacancyStatus.ARCHIVED;
                    pseudoClassStateChanged(ARCHIVED_PSEUDO_CLASS, isArchived);
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleEditVacancy();
                }
            });

            return row;
        });
    }

    private void updateFilters() {
        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String status = statusFilterComboBox.getValue();
        String salaryRange = salaryRangeFilterComboBox.getValue();

        filteredData.setPredicate(vacancy -> {
            if (vacancy == null) {
                return false;
            }

            // 1. Поиск по подстроке в названии, компании или описании
            if (!query.isEmpty()) {
                String title = vacancy.getTitle() != null ? vacancy.getTitle().toLowerCase() : "";
                String company = vacancy.getEmployerCompanyName() != null ? vacancy.getEmployerCompanyName().toLowerCase() : "";
                String desc = vacancy.getDescription() != null ? vacancy.getDescription().toLowerCase() : "";

                boolean matchesQuery = title.contains(query) || company.contains(query) || desc.contains(query);
                if (!matchesQuery) {
                    return false;
                }
            }

            // 2. Фильтр по статусу
            if (status != null && !status.equals("Все статусы")) {
                if (vacancy.getStatus() == null || !vacancy.getStatus().name().equalsIgnoreCase(status)) {
                    return false;
                }
            }

            // 3. Фильтр по зарплатному диапазону
            return matchesSalaryRange(vacancy, salaryRange);
        });

        updateCountLabel();
    }

    /**
     * Проверяет, соответствует ли вакансия критериям фильтрации.
     * Package-private static для тестируемости (используется в VacancyFilterTest).
     */
    static boolean matches(Vacancy vacancy, String query, String status, String salaryRange) {
        if (vacancy == null) {
            return false;
        }

        // 1. Поиск по подстроке
        if (query != null && !query.isEmpty()) {
            String q = query.toLowerCase();
            String title = vacancy.getTitle() != null ? vacancy.getTitle().toLowerCase() : "";
            String company = vacancy.getEmployerCompanyName() != null ? vacancy.getEmployerCompanyName().toLowerCase() : "";
            String desc = vacancy.getDescription() != null ? vacancy.getDescription().toLowerCase() : "";

            if (!title.contains(q) && !company.contains(q) && !desc.contains(q)) {
                return false;
            }
        }

        // 2. Фильтр по статусу
        if (status != null && !status.equals("Все статусы")) {
            if (vacancy.getStatus() == null || !vacancy.getStatus().name().equalsIgnoreCase(status)) {
                return false;
            }
        }

        // 3. Фильтр по зарплатному диапазону
        return matchesSalaryRange(vacancy, salaryRange);
    }

    private static boolean matchesSalaryRange(Vacancy vacancy, String salaryRange) {
        if (salaryRange == null || salaryRange.equals("Вся зарплата")) {
            return true;
        }

        BigDecimal min = vacancy.getSalaryMin();
        if (min == null) {
            return false;
        }

        BigDecimal bound100k = new BigDecimal("100000");
        BigDecimal bound200k = new BigDecimal("200000");

        return switch (salaryRange) {
            case "до 100 000" -> min.compareTo(bound100k) <= 0;
            case "100 000–200 000" -> min.compareTo(bound100k) >= 0 && min.compareTo(bound200k) <= 0;
            case "от 200 000" -> min.compareTo(bound200k) >= 0;
            default -> true;
        };
    }

    private void updateCountLabel() {
        if (countLabel != null) {
            countLabel.setText(String.format("Показано: %d из %d вакансий", filteredData.size(), masterData.size()));
        }
    }

    private void setBusy(boolean busy) {
        vacancyTableView.setDisable(busy);
        refreshButton.setDisable(busy);
        addButton.setDisable(busy);
        if (busy) {
            editButton.setDisable(true);
            deleteButton.setDisable(true);
        } else {
            boolean hasSelection = vacancyTableView.getSelectionModel().getSelectedItem() != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
        }
    }

    private Stage getStage() {
        return (Stage) vacancyTableView.getScene().getWindow();
    }

    // --- Обработчики действий UI ---

    @FXML
    public void handleRefresh() {
        loadDataFromService();
    }

    @FXML
    public void handleResetFilters() {
        searchField.clear();
        statusFilterComboBox.setValue("Все статусы");
        salaryRangeFilterComboBox.setValue("Вся зарплата");
    }

    private void loadDataFromService() {
        if (vacancyService == null) {
            return;
        }
        setBusy(true);
        FxTasks.run(
                () -> vacancyService.getAllVacancies(),
                list -> {
                    setBusy(false);
                    masterData.setAll(list);
                    updateCountLabel();
                },
                error -> {
                    setBusy(false);
                    log.error("Ошибка загрузки вакансий", error);
                    UiUtils.showErrorAlert("Ошибка загрузки", "Не удалось загрузить вакансии: " + error.getMessage());
                }
        );
    }

    @FXML
    public void handleAddVacancy() {
        setBusy(true);
        FxTasks.run(
                () -> userDao.findAllEmployers(),
                employers -> {
                    setBusy(false);
                    Optional<Vacancy> result = VacancyDialog.show(getStage(), null, employers);
                    result.ifPresent(newVacancy -> {
                        setBusy(true);
                        FxTasks.run(
                                () -> vacancyService.createVacancy(newVacancy),
                                created -> {
                                    setBusy(false);
                                    masterData.add(0, created);
                                    vacancyTableView.getSelectionModel().select(created);
                                    updateCountLabel();
                                    UiUtils.showInfoAlert("Успех", "Вакансия '" + created.getTitle() + "' успешно добавлена!");
                                },
                                error -> {
                                    setBusy(false);
                                    UiUtils.showErrorAlert("Ошибка сохранения", error.getMessage());
                                }
                        );
                    });
                },
                error -> {
                    setBusy(false);
                    UiUtils.showErrorAlert("Ошибка получения работодателей", error.getMessage());
                }
        );
    }

    @FXML
    public void handleEditVacancy() {
        Vacancy selected = vacancyTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        setBusy(true);
        FxTasks.run(
                () -> userDao.findAllEmployers(),
                employers -> {
                    setBusy(false);
                    Optional<Vacancy> result = VacancyDialog.show(getStage(), selected, employers);
                    result.ifPresent(editedVacancy -> {
                        setBusy(true);
                        FxTasks.run(
                                () -> {
                                    vacancyService.updateVacancy(editedVacancy);
                                    return editedVacancy;
                                },
                                updated -> {
                                    setBusy(false);
                                    // Обновляем ссылку в masterData для реактивной перерисовки строки
                                    for (int i = 0; i < masterData.size(); i++) {
                                        if (masterData.get(i).getId().equals(updated.getId())) {
                                            masterData.set(i, updated);
                                            break;
                                        }
                                    }
                                    vacancyTableView.refresh();
                                    updateCountLabel();
                                    UiUtils.showInfoAlert("Успех", "Вакансия '" + updated.getTitle() + "' успешно обновлена!");
                                },
                                error -> {
                                    setBusy(false);
                                    UiUtils.showErrorAlert("Ошибка обновления", error.getMessage());
                                }
                        );
                    });
                },
                error -> {
                    setBusy(false);
                    UiUtils.showErrorAlert("Ошибка получения работодателей", error.getMessage());
                }
        );
    }

    @FXML
    public void handleDeleteVacancy() {
        Vacancy selected = vacancyTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean confirmed = UiUtils.showConfirmDialog(
                getStage(),
                "Вы действительно хотите удалить вакансию \"" + selected.getTitle() + "\"?\nЭто действие необратимо."
        );

        if (!confirmed) {
            return;
        }

        setBusy(true);
        FxTasks.run(
                () -> {
                    vacancyService.deleteVacancy(selected.getId());
                    return null;
                },
                ignored -> {
                    setBusy(false);
                    masterData.remove(selected);
                    updateCountLabel();
                    UiUtils.showInfoAlert("Успех", "Вакансия успешно удалена");
                },
                error -> {
                    setBusy(false);
                    UiUtils.showErrorAlert("Ошибка удаления", error.getMessage());
                }
        );
    }

    @FXML
    public void handleShowStatistics() {
        if (statisticsService == null) {
            return;
        }
        setBusy(true);
        FxTasks.run(
                () -> statisticsService.calculateMetrics(),
                dto -> {
                    setBusy(false);
                    StatisticsDialog.show(getStage(), dto);
                },
                error -> {
                    setBusy(false);
                    log.error("Ошибка расчета аналитики", error);
                    UiUtils.showErrorAlert("Ошибка статистики", "Не удалось рассчитать метрики: " + error.getMessage());
                }
        );
    }

    @FXML
    public void handleExportCsv() {
        if (csvExportService == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить выгрузку вакансий в CSV");
        fileChooser.setInitialFileName("vacancies_export.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv"));

        File targetFile = fileChooser.showSaveDialog(getStage());
        if (targetFile == null) {
            return;
        }

        // Захват снимка данных (snapshot) на UI-потоке для потокобезопасности
        List<Vacancy> snapshot = new ArrayList<>(filteredData);

        exportButton.setDisable(true);
        FxTasks.run(
                () -> {
                    csvExportService.export(snapshot, targetFile);
                    return null;
                },
                ignored -> {
                    exportButton.setDisable(false);
                    UiUtils.showInfoAlert(
                            "Экспорт завершен",
                            "Успешно выгружено " + snapshot.size() + " вакансий в файл:\n" + targetFile.getAbsolutePath()
                    );
                },
                error -> {
                    exportButton.setDisable(false);
                    log.error("Ошибка экспорта CSV", error);
                    UiUtils.showErrorAlert("Ошибка экспорта", "Не удалось выгрузить данные: " + error.getMessage());
                }
        );
    }
}
