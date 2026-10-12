package ru.mirea.hrsystem.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.hrsystem.model.StatisticsDto;

import java.math.BigDecimal;
import java.util.Locale;

public final class StatisticsDialog {
    private StatisticsDialog() {
    }

    public static void show(Window owner, StatisticsDto dto) {
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setTitle("Статистика");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        addCard(grid, 0, 0, "Всего вакансий", Long.toString(dto.totalVacancies()));
        addCard(grid, 1, 0, "Активных вакансий", Long.toString(dto.activeVacancies()));
        addCard(grid, 2, 0, "В архиве", Long.toString(dto.archivedVacancies()));
        addCard(grid, 0, 1, "Средняя зарплата от", money(dto.averageSalaryMin()));
        addCard(grid, 1, 1, "Максимальная зарплата", money(dto.maxSalary()));

        Button close = new Button("Закрыть");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(event -> stage.close());

        VBox root = new VBox(16, grid, close);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        if (owner != null && owner.getScene() != null) {
            root.getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private static void addCard(GridPane grid, int column, int row, String title, String value) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("metric-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        VBox card = new VBox(6, valueLabel, titleLabel);
        card.getStyleClass().add("metric-card");
        grid.add(card, column, row);
    }

    private static String money(BigDecimal amount) {
        long value = amount == null ? 0 : amount.longValue();
        return String.format(Locale.forLanguageTag("ru-RU"), "%,d ₽", value);
    }
}
