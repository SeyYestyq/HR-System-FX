package ru.mirea.hrsystem.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private static final BigDecimal SALARY_LOW = new BigDecimal("100000");
    private static final BigDecimal SALARY_HIGH = new BigDecimal("200000");
    private static final PseudoClass ARCHIVED = PseudoClass.getPseudoClass("archived");

    @FXML private Label dbStatusLabel;
    @FXML private Label countLabel;
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
    @FXML private Button refreshButton;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button exportButton;

    private final ObservableList<Vacancy> masterData = FXCollections.observableArrayList();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanProperty exporting = new SimpleBooleanProperty(false);

    private FilteredList<Vacancy> filteredData;
    private VacancyService vacancyService;
    private UserDao userDao;
    private StatisticsService statisticsService;
    private CsvExportService csvExportService;
    private List<User> employers = List.of();

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        setupRowStyle();
        bindButtons();
        vacancyTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !busy.get()) {
                handleEditVacancy();
            }
        });
    }

    public void initDependencies(VacancyService vacancyService, UserDao userDao,
                                 StatisticsService statisticsService, CsvExportService csvExportService) {
        this.vacancyService = vacancyService;
        this.userDao = userDao;
        this.statisticsService = statisticsService;
        this.csvExportService = csvExportService;
        loadData();
    }

    public void setDbStatus(boolean connected) {
        dbStatusLabel.setText(connected
                ? "🟢 PostgreSQL подключен"
                : "🟠 Автономный режим (Stub)");
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        statusFilterComboBox.getSelectionModel().selectFirst();
        salaryRangeFilterComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleAddVacancy() {
        openVacancyDialog(null);
    }

    @FXML
    private void handleEditVacancy() {
        Vacancy selected = vacancyTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openVacancyDialog(selected);
        }
    }

    @FXML
    private void handleDeleteVacancy() {
        Vacancy selected = vacancyTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String title = selected.getTitle() == null ? "" : selected.getTitle();
        boolean confirmed = UiUtils.showConfirmDialog(window(),
                "Вы уверены, что хотите удалить вакансию \"" + title + "\"? Это действие необратимо.");
        if (!confirmed) {
            return;
        }
        Long id = selected.getId();
        setBusy(true);
        FxTasks.run(() -> {
            vacancyService.deleteVacancy(id);
            return null;
        }, unused -> {
            setBusy(false);
            loadData();
        }, error -> {
            setBusy(false);
            showError("Ошибка удаления", error);
        });
    }

    @FXML
    private void handleShowStatistics() {
        if (busy.get()) {
            return;
        }
        setBusy(true);
        FxTasks.run(statisticsService::calculateMetrics, dto -> {
            setBusy(false);
            StatisticsDialog.show(window(), dto);
        }, error -> {
            setBusy(false);
            showError("Ошибка статистики", error);
        });
    }

    @FXML
    private void handleExportCsv() {
        if (exporting.get() || busy.get()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить выгрузку вакансий в CSV");
        chooser.setInitialFileName("vacancies_export.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv"));
        File target = chooser.showSaveDialog(window());
        if (target == null) {
            return;
        }
        List<Vacancy> snapshot = new ArrayList<>(filteredData);
        exporting.set(true);
        FxTasks.run(() -> {
            csvExportService.export(snapshot, target);
            return target;
        }, file -> {
            exporting.set(false);
            UiUtils.showInfoAlert("Экспорт завершен", file.getAbsolutePath());
        }, error -> {
            exporting.set(false);
            showError("Ошибка экспорта", error);
        });
    }

    static boolean matches(Vacancy vacancy, String query, String status, String salaryRange) {
        if (vacancy == null) {
            return false;
        }
        return matchesQuery(vacancy, query)
                && matchesStatus(vacancy, status)
                && matchesSalaryRange(vacancy, salaryRange);
    }

    private void setupColumns() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        titleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getTitle()));
        employerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(employerName(cell.getValue())));
        salaryMinColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getSalaryMin()));
        salaryMaxColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getSalaryMax()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(statusName(cell.getValue())));
    }

    private void setupFilters() {
        statusFilterComboBox.getItems().setAll("Все статусы", "ACTIVE", "ARCHIVED", "REJECTED");
        statusFilterComboBox.getSelectionModel().selectFirst();
        salaryRangeFilterComboBox.getItems().setAll(
                "Вся зарплата", "до 100 000", "100 000–200 000", "от 200 000");
        salaryRangeFilterComboBox.getSelectionModel().selectFirst();

        filteredData = new FilteredList<>(masterData, vacancy -> true);
        SortedList<Vacancy> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(vacancyTableView.comparatorProperty());
        vacancyTableView.setItems(sorted);

        filteredData.predicateProperty().bind(Bindings.createObjectBinding(
                () -> vacancy -> matches(
                        vacancy,
                        searchField.getText(),
                        statusFilterComboBox.getValue(),
                        salaryRangeFilterComboBox.getValue()),
                searchField.textProperty(),
                statusFilterComboBox.valueProperty(),
                salaryRangeFilterComboBox.valueProperty()));
        filteredData.addListener((ListChangeListener<Vacancy>) change -> updateCount());
        updateCount();
    }

    private void setupRowStyle() {
        vacancyTableView.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Vacancy item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(ARCHIVED, !empty && item != null && item.getStatus() == VacancyStatus.ARCHIVED);
            }
        });
    }

    private void bindButtons() {
        refreshButton.disableProperty().bind(busy);
        addButton.disableProperty().bind(busy);
        editButton.disableProperty().bind(busy.or(vacancyTableView.getSelectionModel().selectedItemProperty().isNull()));
        deleteButton.disableProperty().bind(busy.or(vacancyTableView.getSelectionModel().selectedItemProperty().isNull()));
        exportButton.disableProperty().bind(busy.or(exporting));
        vacancyTableView.disableProperty().bind(busy);
    }

    private void loadData() {
        setBusy(true);
        FxTasks.run(
                () -> new PageData(vacancyService.getAllVacancies(), userDao.findAllEmployers()),
                page -> {
                    employers = page.employers();
                    masterData.setAll(page.vacancies());
                    updateCount();
                    setBusy(false);
                },
                error -> {
                    setBusy(false);
                    showError("Ошибка загрузки", error);
                });
    }

    private void openVacancyDialog(Vacancy vacancy) {
        setBusy(true);
        FxTasks.run(userDao::findAllEmployers, list -> {
            setBusy(false);
            employers = list;
            try {
                VacancyDialog.show(window(), vacancy, employers);
                loadData();
            } catch (Exception ex) {
                showError("Диалог вакансии", ex);
            }
        }, error -> {
            setBusy(false);
            showError("Ошибка загрузки", error);
        });
    }

    private void setBusy(boolean value) {
        busy.set(value);
    }

    private void updateCount() {
        int shown = filteredData == null ? 0 : filteredData.size();
        countLabel.setText("Показано: " + shown + " из " + masterData.size() + " вакансий");
    }

    private Window window() {
        return vacancyTableView.getScene().getWindow();
    }

    private void showError(String title, Throwable error) {
        log.error(title, error);
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        UiUtils.showErrorAlert(title, message);
    }

    private static boolean matchesQuery(Vacancy vacancy, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return true;
        }
        return contains(vacancy.getTitle(), normalized)
                || contains(vacancy.getEmployerCompanyName(), normalized)
                || contains(vacancy.getDescription(), normalized);
    }

    private static boolean matchesStatus(Vacancy vacancy, String status) {
        if (status == null || status.isBlank() || "Все статусы".equals(status)) {
            return true;
        }
        return vacancy.getStatus() != null && vacancy.getStatus().name().equals(status);
    }

    private static boolean matchesSalaryRange(Vacancy vacancy, String salaryRange) {
        if (salaryRange == null || salaryRange.isBlank() || "Вся зарплата".equals(salaryRange)) {
            return true;
        }
        BigDecimal salary = vacancy.getSalaryMin();
        if (salary == null) {
            return false;
        }
        return switch (salaryRange) {
            case "до 100 000" -> salary.compareTo(SALARY_LOW) <= 0;
            case "100 000–200 000" -> salary.compareTo(SALARY_LOW) >= 0 && salary.compareTo(SALARY_HIGH) <= 0;
            case "от 200 000" -> salary.compareTo(SALARY_HIGH) >= 0;
            default -> true;
        };
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String employerName(Vacancy vacancy) {
        String company = vacancy.getEmployerCompanyName();
        if (company != null && !company.isBlank()) {
            return company;
        }
        return "ID #" + vacancy.getEmployerId();
    }

    private static String statusName(Vacancy vacancy) {
        return vacancy.getStatus() == null ? "" : vacancy.getStatus().name();
    }

    private record PageData(List<Vacancy> vacancies, List<User> employers) {
    }
}
