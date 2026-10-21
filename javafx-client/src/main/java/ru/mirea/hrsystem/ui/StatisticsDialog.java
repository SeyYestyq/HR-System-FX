package ru.mirea.hrsystem.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.hrsystem.model.StatisticsDto;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class StatisticsDialog {

    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
        symbols.setGroupingSeparator(' ');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    public static void show(Window owner, StatisticsDto dto) {
        if (dto == null) {
            dto = StatisticsDto.empty();
        }

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Аналитический дашборд — Статистика вакансий");
        stage.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        Label headerTitle = new Label("Ключевые метрики рынка труда");
        headerTitle.getStyleClass().add("header-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        VBox totalCard = createMetricCard("Всего вакансий", String.valueOf(dto.totalVacancies()));
        VBox activeCard = createMetricCard("Активных вакансий", String.valueOf(dto.activeVacancies()));
        VBox archivedCard = createMetricCard("В архиве", String.valueOf(dto.archivedVacancies()));

        String avgSalaryText = formatMoney(dto.averageSalaryMin());
        VBox avgSalaryCard = createMetricCard("Средняя зарплата от", avgSalaryText);

        String maxSalaryText = formatMoney(dto.maxSalary());
        VBox maxSalaryCard = createMetricCard("Максимальная зарплата", maxSalaryText);

        grid.add(totalCard, 0, 0);
        grid.add(activeCard, 1, 0);
        grid.add(archivedCard, 2, 0);
        grid.add(avgSalaryCard, 0, 1);
        grid.add(maxSalaryCard, 1, 1, 2, 1);

        Button closeButton = new Button("Закрыть");
        closeButton.getStyleClass().add("secondary-button");
        closeButton.setPrefWidth(120);
        closeButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(headerTitle, new Separator(), grid, buttonBox);

        Scene scene = new Scene(root);
        String cssPath = StatisticsDialog.class.getResource("/ru/mirea/hrsystem/css/app.css") != null
                ? StatisticsDialog.class.getResource("/ru/mirea/hrsystem/css/app.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        stage.setScene(scene);
        stage.showAndWait();
    }

    private static VBox createMetricCard(String title, String value) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");
        card.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("metric-value");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "0 ₽";
        }
        return CURRENCY_FORMAT.format(amount) + " ₽";
    }
}
