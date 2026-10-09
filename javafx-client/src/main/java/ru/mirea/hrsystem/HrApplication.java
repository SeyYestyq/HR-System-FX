package ru.mirea.hrsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.repository.*;
import ru.mirea.hrsystem.service.CsvExportService;
import ru.mirea.hrsystem.service.StatisticsService;
import ru.mirea.hrsystem.service.VacancyService;
import ru.mirea.hrsystem.ui.MainController;
import ru.mirea.hrsystem.util.DatabaseManager;

import java.util.List;
import java.util.Optional;

public class HrApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(HrApplication.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Запуск приложения HireSystem JavaFX Client");

        DatabaseManager dbManager = new DatabaseManager();
        boolean dbAvailable = dbManager.testConnection();

        VacancyDao vacancyDao;
        UserDao userDao;

        if (dbAvailable) {
            log.info("Успешное подключение к PostgreSQL! Используются реальные JDBC DAO.");
            vacancyDao = new PostgresVacancyDao(dbManager);
            userDao = new PostgresUserDao(dbManager);
        } else {
            log.warn("PostgreSQL недоступен. Активирован автономный режим со стабами (InMemoryVacancyDao).");
            vacancyDao = new InMemoryVacancyDao();
            userDao = new UserDao() {
                private final List<User> mockEmployers = List.of(
                        new User(1L, "yandex_hr@yandex.ru", "Анна Смирнова", "EMPLOYER", "Яндекс"),
                        new User(2L, "sber_hr@sber.ru", "Иван Ковалев", "EMPLOYER", "СберТех"),
                        new User(3L, "tinkoff_hr@tbank.ru", "Елена Романова", "EMPLOYER", "Т-Банк"),
                        new User(4L, "ozon_hr@ozon.ru", "Дмитрий Волков", "EMPLOYER", "Озон")
                );

                @Override
                public List<User> findAllEmployers() {
                    return mockEmployers;
                }

                @Override
                public Optional<User> findById(Long id) {
                    return mockEmployers.stream().filter(u -> u.getId().equals(id)).findFirst();
                }
            };
        }

        VacancyService vacancyService = new VacancyService(vacancyDao);
        StatisticsService statisticsService = new StatisticsService(vacancyDao);
        CsvExportService csvExportService = new CsvExportService();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/mirea/hrsystem/view/MainView.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.initDependencies(vacancyService, userDao, statisticsService, csvExportService);
        controller.setDbStatus(dbAvailable);

        Scene scene = new Scene(root, 1080, 680);
        primaryStage.setTitle("HireSystem — Управление вакансиями (JavaFX 21)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(500);

        primaryStage.show();
        log.info("Главное окно успешно отображено");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
