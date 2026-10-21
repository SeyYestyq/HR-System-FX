package ru.mirea.hrsystem.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    private Vacancy existingVacancy;

    @FXML
    public void initialize() {
        if (statusComboBox != null) {
            statusComboBox.getItems().setAll(VacancyStatus.values());
            statusComboBox.setValue(VacancyStatus.ACTIVE);
        }
        if (employerComboBox != null) {
            employerComboBox.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(User user) {
                    if (user == null) {
                        return "";
                    }
                    String name = (user.getCompanyName() != null && !user.getCompanyName().isBlank())
                            ? user.getCompanyName()
                            : user.getFullName();
                    return name + " (" + user.getEmail() + ")";
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
            titleField.setText(vacancy.getTitle());
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
            employerComboBox.getItems().stream()
                    .filter(u -> vacancy.getEmployerId().equals(u.getId()))
                    .findFirst()
                    .ifPresent(employerComboBox::setValue);
        }
    }

    public boolean validate() {
        clearErrorStyles();
        if (titleField == null || titleField.getText() == null || titleField.getText().isBlank()){
            showError("Название вакансии обязательно для заполнения", titleField);
            return false;
        }
        if (employerComboBox == null || employerComboBox.getValue() == null) {
            showError("Выберите компанию-работодателя", employerComboBox);
            return false;
        }
        BigDecimal min = null;
        BigDecimal max = null;
        if (salaryMinField != null && salaryMinField.getText() != null && !salaryMinField.getText().isBlank()) {
            try {
                min = new BigDecimal(salaryMinField.getText().trim().replace(" ", ""));
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    showError("Зарплата 'от' не может быть отрицательной", salaryMinField);
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Зарплата 'от' должна быть числом", salaryMinField);
                return false;
            }
        }
        if (salaryMaxField != null && salaryMaxField.getText() != null && !salaryMaxField.getText().isBlank()) {
            try {
                max = new BigDecimal(salaryMaxField.getText().trim().replace(" ", ""));
                if (max.compareTo(BigDecimal.ZERO) < 0) {
                    showError("Зарплата 'до' не может быть отрицательной", salaryMaxField);
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Зарплата 'до' должна быть числом", salaryMaxField);
                return false;
            }
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            showError("Зарплата 'от' не может превышать зарплату 'до'", salaryMinField);
            return false;
        }
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        return true;
    }

    private void showError(String message, Control field) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
        if (field != null) {
            field.setStyle("-fx-border-color: #c73545; -fx-border-width: 1.5px; -fx-border-radius: 3px");
            field.requestFocus();
        }
    }

    private void clearErrorStyles() {
        if (titleField != null) titleField.setStyle("");
        if (employerComboBox != null) employerComboBox.setStyle("");
        if (salaryMinField != null) salaryMinField.setStyle("");
        if (salaryMaxField != null) salaryMaxField.setStyle("");
    }

    public Vacancy buildVacancy() {
        Vacancy v = existingVacancy != null ? existingVacancy : new Vacancy();

        v.setTitle(titleField != null && titleField.getText() != null ? titleField.getText().trim() : "");
        v.setDescription(descriptionArea != null && descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");

        String minText = salaryMinField != null && salaryMinField.getText() != null ? salaryMinField.getText().trim().replace(" ", "") : "";
        v.setSalaryMin(!minText.isEmpty() ? new BigDecimal(minText) : null);

        String maxText = salaryMaxField != null && salaryMaxField.getText() != null ? salaryMaxField.getText().trim().replace(" ", "") : "";
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
