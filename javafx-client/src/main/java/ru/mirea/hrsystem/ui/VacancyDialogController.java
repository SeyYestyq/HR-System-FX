package ru.mirea.hrsystem.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * FXML-контроллер модального диалога создания и редактирования вакансии.
 * Выполняет привязку UI-элементов, предварительное заполнение формы
 * и клиентскую валидацию ввода перед закрытием диалога.
 */
public class VacancyDialogController {

    @FXML private TextField titleField;
    @FXML private ComboBox<User> employerComboBox;
    @FXML private TextField salaryMinField;
    @FXML private TextField salaryMaxField;
    @FXML private ComboBox<VacancyStatus> statusComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private Label errorLabel;

    private Vacancy existingVacancy;

    @FXML
    public void initialize() {
        if (statusComboBox != null) {
            statusComboBox.getItems().setAll(VacancyStatus.values());
            statusComboBox.setValue(VacancyStatus.ACTIVE);
        }

        if (employerComboBox != null) {
            employerComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(User user) {
                    if (user == null) {
                        return "";
                    }
                    if (user.getCompanyName() != null && !user.getCompanyName().isBlank()) {
                        return user.getCompanyName() + " (" + user.getEmail() + ")";
                    }
                    return user.getFullName() + " (" + user.getEmail() + ")";
                }

                @Override
                public User fromString(String string) {
                    return null;
                }
            });
        }

        if (errorLabel != null) {
            errorLabel.setText("");
        }

        setupValidationReset();
    }

    private void setupValidationReset() {
        if (titleField != null) {
            titleField.textProperty().addListener((obs, o, n) -> clearError());
        }
        if (employerComboBox != null) {
            employerComboBox.valueProperty().addListener((obs, o, n) -> clearError());
        }
        if (salaryMinField != null) {
            salaryMinField.textProperty().addListener((obs, o, n) -> clearError());
        }
        if (salaryMaxField != null) {
            salaryMaxField.textProperty().addListener((obs, o, n) -> clearError());
        }
    }

    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        resetFieldStyles();
    }

    private void resetFieldStyles() {
        if (titleField != null) titleField.setStyle("");
        if (employerComboBox != null) employerComboBox.setStyle("");
        if (salaryMinField != null) salaryMinField.setStyle("");
        if (salaryMaxField != null) salaryMaxField.setStyle("");
    }

    public void setEmployers(List<User> employers) {
        if (employerComboBox != null && employers != null) {
            employerComboBox.getItems().setAll(employers);
            if (!employers.isEmpty() && employerComboBox.getValue() == null) {
                employerComboBox.setValue(employers.get(0));
            }
        }
    }

    public void setVacancy(Vacancy vacancy) {
        this.existingVacancy = vacancy;
        if (vacancy == null) {
            return;
        }

        if (titleField != null) {
            titleField.setText(vacancy.getTitle() != null ? vacancy.getTitle() : "");
        }
        if (descriptionArea != null) {
            descriptionArea.setText(vacancy.getDescription() != null ? vacancy.getDescription() : "");
        }
        if (salaryMinField != null) {
            salaryMinField.setText(vacancy.getSalaryMin() != null ? vacancy.getSalaryMin().toPlainString() : "");
        }
        if (salaryMaxField != null) {
            salaryMaxField.setText(vacancy.getSalaryMax() != null ? vacancy.getSalaryMax().toPlainString() : "");
        }
        if (statusComboBox != null && vacancy.getStatus() != null) {
            statusComboBox.setValue(vacancy.getStatus());
        }

        if (employerComboBox != null && vacancy.getEmployerId() != null) {
            for (User u : employerComboBox.getItems()) {
                if (u.getId() != null && u.getId().equals(vacancy.getEmployerId())) {
                    employerComboBox.setValue(u);
                    break;
                }
            }
        }
    }

    /**
     * Валидирует заполнение полей формы.
     * Возвращает true, если все поля корректны, иначе выводит текст ошибки и возвращает false.
     */
    public boolean validate() {
        resetFieldStyles();

        String title = titleField != null && titleField.getText() != null ? titleField.getText().trim() : "";
        if (title.length() < 3 || title.length() > 100) {
            showError("Название вакансии обязательно (от 3 до 100 символов)", titleField);
            return false;
        }

        User employer = employerComboBox != null ? employerComboBox.getValue() : null;
        if (employer == null) {
            showError("Необходимо выбрать работодателя (компанию)", employerComboBox);
            return false;
        }

        BigDecimal minSalary = null;
        String minText = salaryMinField != null && salaryMinField.getText() != null ? salaryMinField.getText().trim() : "";
        if (!minText.isEmpty()) {
            try {
                minSalary = new BigDecimal(minText);
                if (minSalary.compareTo(BigDecimal.ZERO) < 0) {
                    showError("Минимальная зарплата не может быть отрицательной", salaryMinField);
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Минимальная зарплата должна быть числом", salaryMinField);
                return false;
            }
        }

        BigDecimal maxSalary = null;
        String maxText = salaryMaxField != null && salaryMaxField.getText() != null ? salaryMaxField.getText().trim() : "";
        if (!maxText.isEmpty()) {
            try {
                maxSalary = new BigDecimal(maxText);
                if (maxSalary.compareTo(BigDecimal.ZERO) < 0) {
                    showError("Максимальная зарплата не может быть отрицательной", salaryMaxField);
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Максимальная зарплата должна быть числом", salaryMaxField);
                return false;
            }
        }

        if (minSalary != null && maxSalary != null && maxSalary.compareTo(minSalary) < 0) {
            showError("Максимальная зарплата (" + maxSalary + ") не может быть меньше минимальной (" + minSalary + ")", salaryMaxField);
            return false;
        }

        clearError();
        return true;
    }

    private void showError(String message, Control faultyControl) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
        if (faultyControl != null) {
            faultyControl.setStyle("-fx-border-color: #c73545; -fx-focus-color: #c73545;");
            faultyControl.requestFocus();
        }
    }

    /**
     * Создает или обновляет модель Vacancy из значений формы.
     */
    public Vacancy buildVacancy() {
        Vacancy v = existingVacancy != null ? existingVacancy : new Vacancy();

        v.setTitle(titleField.getText().trim());
        v.setDescription(descriptionArea != null && descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");

        String minText = salaryMinField != null && salaryMinField.getText() != null ? salaryMinField.getText().trim() : "";
        v.setSalaryMin(!minText.isEmpty() ? new BigDecimal(minText) : null);

        String maxText = salaryMaxField != null && salaryMaxField.getText() != null ? salaryMaxField.getText().trim() : "";
        v.setSalaryMax(!maxText.isEmpty() ? new BigDecimal(maxText) : null);

        v.setStatus(statusComboBox != null && statusComboBox.getValue() != null ? statusComboBox.getValue() : VacancyStatus.ACTIVE);

        User employer = employerComboBox != null ? employerComboBox.getValue() : null;
        if (employer != null) {
            v.setEmployerId(employer.getId());
            String company = employer.getCompanyName();
            if (company == null || company.isBlank()) {
                company = employer.getFullName();
            }
            if (company == null || company.isBlank()) {
                company = employer.getEmail();
            }
            v.setEmployerCompanyName(company);
        }

        return v;
    }
}
