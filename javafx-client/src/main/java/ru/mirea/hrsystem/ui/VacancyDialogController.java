package ru.mirea.hrsystem.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;

import java.math.BigDecimal;
import java.util.List;

public class VacancyDialogController {
    @FXML private TextField titleField;
    @FXML private ComboBox<User> employerComboBox;
    @FXML private TextField salaryMinField;
    @FXML private TextField salaryMaxField;
    @FXML private ComboBox<VacancyStatus> statusComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Vacancy existing;
    private Vacancy result;

    @FXML
    public void initialize() {
        statusComboBox.getItems().setAll(VacancyStatus.values());
        statusComboBox.setValue(VacancyStatus.ACTIVE);
        saveButton.addEventFilter(ActionEvent.ACTION, this::onSave);
        cancelButton.setOnAction(event -> close());
    }

    void setData(Vacancy vacancy, List<User> employers) {
        existing = vacancy;
        employerComboBox.getItems().setAll(employers);
        if (vacancy == null) {
            return;
        }
        titleField.setText(vacancy.getTitle());
        descriptionArea.setText(vacancy.getDescription());
        salaryMinField.setText(text(vacancy.getSalaryMin()));
        salaryMaxField.setText(text(vacancy.getSalaryMax()));
        if (vacancy.getStatus() != null) {
            statusComboBox.setValue(vacancy.getStatus());
        }
        employers.stream()
                .filter(user -> user.getId().equals(vacancy.getEmployerId()))
                .findFirst()
                .ifPresent(employerComboBox::setValue);
    }

    Vacancy getResult() {
        return result;
    }

    private void onSave(ActionEvent event) {
        if (!validate()) {
            event.consume();
            return;
        }
        result = buildVacancy();
        close();
    }

    private boolean validate() {
        boolean titleOk = titleField.getText() != null && titleField.getText().trim().length() >= 3
                && titleField.getText().trim().length() <= 100;
        boolean employerOk = employerComboBox.getValue() != null;
        BigDecimal min = readMoney(salaryMinField);
        BigDecimal max = readMoney(salaryMaxField);
        boolean minOk = moneyOk(salaryMinField, min);
        boolean maxOk = moneyOk(salaryMaxField, max) && (min == null || max == null || max.compareTo(min) >= 0);

        mark(titleField, !titleOk);
        mark(employerComboBox, !employerOk);
        mark(salaryMinField, !minOk);
        mark(salaryMaxField, !maxOk);

        if (titleOk && employerOk && minOk && maxOk) {
            errorLabel.setText("");
            return true;
        }
        errorLabel.setText("Проверьте название (3–100 символов), компанию и зарплату.");
        return false;
    }

    private Vacancy buildVacancy() {
        Vacancy vacancy = new Vacancy();
        if (existing != null) {
            vacancy.setId(existing.getId());
        }
        vacancy.setTitle(titleField.getText().trim());
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        vacancy.setDescription(description.isEmpty() ? null : description);
        vacancy.setSalaryMin(readMoney(salaryMinField));
        vacancy.setSalaryMax(readMoney(salaryMaxField));
        vacancy.setStatus(statusComboBox.getValue() == null ? VacancyStatus.ACTIVE : statusComboBox.getValue());
        User employer = employerComboBox.getValue();
        vacancy.setEmployerId(employer.getId());
        vacancy.setEmployerCompanyName(employer.getCompanyName());
        return vacancy;
    }

    private static BigDecimal readMoney(TextField field) {
        String text = field.getText() == null ? "" : field.getText().trim().replace(" ", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean moneyOk(TextField field, BigDecimal value) {
        String text = field.getText() == null ? "" : field.getText().trim();
        if (text.isEmpty()) {
            return true;
        }
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }

    private static void mark(javafx.scene.Node node, boolean invalid) {
        node.getStyleClass().remove("field-error");
        if (invalid) {
            node.getStyleClass().add("field-error");
        }
    }

    private static String text(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private void close() {
        saveButton.getScene().getWindow().hide();
    }
}
