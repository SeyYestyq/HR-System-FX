package ru.mirea.hrsystem.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class VacancyDialogController {

    private static final DecimalFormat CURRENCY_FORMAT;
    private static final int TITLE_MIN_LENGTH = 3;
    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final BigDecimal MAX_SALARY = new BigDecimal("999999999");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
        symbols.setGroupingSeparator(' ');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    @FXML private TextField titleField;
    @FXML private Label titleErrorLabel;
    @FXML private Label titleCountLabel;

    @FXML private ComboBox<User> employerComboBox;
    @FXML private Label employerErrorLabel;

    @FXML private TextField salaryMinField;
    @FXML private Label salaryMinErrorLabel;

    @FXML private TextField salaryMaxField;
    @FXML private Label salaryMaxErrorLabel;

    @FXML private ComboBox<VacancyStatus> statusComboBox;

    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label descriptionCountLabel;

    @FXML private Label errorLabel;

    private Button okButton;
    private Vacancy existingVacancy;
    private boolean userInteracted = false;

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

        setupLiveListeners();
    }

    public void setOkButton(Button okButton) {
        this.okButton = okButton;
        validateRealtime();
    }

    private void setupLiveListeners() {
        if (titleField != null) {
            titleField.textProperty().addListener((obs, oldVal, newVal) -> {
                userInteracted = true;
                validateRealtime();
            });
            titleField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    userInteracted = true;
                    validateRealtime();
                }
            });
        }

        if (employerComboBox != null) {
            employerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                userInteracted = true;
                validateRealtime();
            });
        }

        if (salaryMinField != null) {
            salaryMinField.textProperty().addListener((obs, oldVal, newVal) -> {
                userInteracted = true;
                validateRealtime();
            });
        }

        if (salaryMaxField != null) {
            salaryMaxField.textProperty().addListener((obs, oldVal, newVal) -> {
                userInteracted = true;
                validateRealtime();
            });
        }

        if (descriptionArea != null) {
            descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
                validateRealtime();
            });
        }
    }

    public void setEmployers(List<User> employers) {
        if (employerComboBox != null && employers != null) {
            employerComboBox.getItems().setAll(employers);
            if (!employers.isEmpty() && employerComboBox.getValue() == null) {
                employerComboBox.setValue(employers.get(0));
            }
        }
        validateRealtime();
    }

    public void setVacancy(Vacancy vacancy) {
        this.existingVacancy = vacancy;
        if (vacancy == null) {
            updateCharacterCounters();
            validateRealtime();
            return;
        }

        userInteracted = true;

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

        updateCharacterCounters();
        validateRealtime();
    }

    public boolean validate() {
        userInteracted = true;
        return validateRealtime();
    }

    public boolean validateRealtime() {
        boolean valid = true;
        String firstErrorMessage = null;

        updateCharacterCounters();

        // 1. Title validation: mandatory, length between 3 and 100
        String rawTitle = titleField != null && titleField.getText() != null ? titleField.getText() : "";
        String trimmedTitle = rawTitle.trim();
        if (trimmedTitle.isEmpty()) {
            valid = false;
            if (userInteracted) {
                setFieldError(titleField, titleErrorLabel, "Название обязательно (от 3 до 100 символов)");
                if (firstErrorMessage == null) firstErrorMessage = "Название вакансии обязательно для заполнения";
            } else {
                clearFieldError(titleField, titleErrorLabel);
            }
        } else if (trimmedTitle.length() < TITLE_MIN_LENGTH) {
            valid = false;
            setFieldError(titleField, titleErrorLabel, "Слишком короткое (минимум 3 символа, сейчас: " + trimmedTitle.length() + ")");
            if (firstErrorMessage == null) firstErrorMessage = "Название вакансии должно содержать минимум 3 символа";
        } else if (rawTitle.length() > TITLE_MAX_LENGTH) {
            valid = false;
            setFieldError(titleField, titleErrorLabel, "Превышена длина: максимум 100 символов (сейчас: " + rawTitle.length() + ")");
            if (firstErrorMessage == null) firstErrorMessage = "Название вакансии не может превышать 100 символов";
        } else {
            clearFieldError(titleField, titleErrorLabel);
        }

        // 2. Employer validation: mandatory selection
        if (employerComboBox == null || employerComboBox.getValue() == null) {
            valid = false;
            if (userInteracted) {
                setFieldError(employerComboBox, employerErrorLabel, "Выберите компанию-работодателя");
                if (firstErrorMessage == null) firstErrorMessage = "Необходимо выбрать работодателя";
            } else {
                clearFieldError(employerComboBox, employerErrorLabel);
            }
        } else {
            clearFieldError(employerComboBox, employerErrorLabel);
        }

        // 3. Salary Min validation
        BigDecimal min = null;
        boolean minFormatValid = true;
        if (salaryMinField != null && salaryMinField.getText() != null && !salaryMinField.getText().isBlank()) {
            String text = salaryMinField.getText().trim().replace(" ", "");
            try {
                min = new BigDecimal(text);
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    valid = false;
                    minFormatValid = false;
                    setFieldError(salaryMinField, salaryMinErrorLabel, "Зарплата не может быть отрицательной");
                    if (firstErrorMessage == null) firstErrorMessage = "Зарплата 'от' не может быть отрицательной";
                } else if (min.compareTo(MAX_SALARY) > 0) {
                    valid = false;
                    minFormatValid = false;
                    setFieldError(salaryMinField, salaryMinErrorLabel, "Сумма не может превышать 999 999 999 ₽");
                    if (firstErrorMessage == null) firstErrorMessage = "Зарплата 'от' не может превышать 999 999 999 ₽";
                } else {
                    clearFieldError(salaryMinField, salaryMinErrorLabel);
                }
            } catch (NumberFormatException e) {
                valid = false;
                minFormatValid = false;
                setFieldError(salaryMinField, salaryMinErrorLabel, "Введите числовое значение (например, 120000)");
                if (firstErrorMessage == null) firstErrorMessage = "Зарплата 'от' должна быть числом";
            }
        } else {
            clearFieldError(salaryMinField, salaryMinErrorLabel);
        }

        // 4. Salary Max validation
        BigDecimal max = null;
        boolean maxFormatValid = true;
        if (salaryMaxField != null && salaryMaxField.getText() != null && !salaryMaxField.getText().isBlank()) {
            String text = salaryMaxField.getText().trim().replace(" ", "");
            try {
                max = new BigDecimal(text);
                if (max.compareTo(BigDecimal.ZERO) < 0) {
                    valid = false;
                    maxFormatValid = false;
                    setFieldError(salaryMaxField, salaryMaxErrorLabel, "Зарплата не может быть отрицательной");
                    if (firstErrorMessage == null) firstErrorMessage = "Зарплата 'до' не может быть отрицательной";
                } else if (max.compareTo(MAX_SALARY) > 0) {
                    valid = false;
                    maxFormatValid = false;
                    setFieldError(salaryMaxField, salaryMaxErrorLabel, "Сумма не может превышать 999 999 999 ₽");
                    if (firstErrorMessage == null) firstErrorMessage = "Зарплата 'до' не может превышать 999 999 999 ₽";
                } else {
                    clearFieldError(salaryMaxField, salaryMaxErrorLabel);
                }
            } catch (NumberFormatException e) {
                valid = false;
                maxFormatValid = false;
                setFieldError(salaryMaxField, salaryMaxErrorLabel, "Введите числовое значение (например, 200000)");
                if (firstErrorMessage == null) firstErrorMessage = "Зарплата 'до' должна быть числом";
            }
        } else {
            clearFieldError(salaryMaxField, salaryMaxErrorLabel);
        }

        // 5. Cross-field salary range validation (min <= max)
        if (minFormatValid && maxFormatValid && min != null && max != null) {
            if (min.compareTo(max) > 0) {
                valid = false;
                String msg = "Зарплата 'до' (" + formatMoney(max) + ") меньше 'от' (" + formatMoney(min) + ")";
                setFieldError(salaryMaxField, salaryMaxErrorLabel, msg);
                setControlErrorStyle(salaryMinField, true);
                if (firstErrorMessage == null) firstErrorMessage = "Зарплата 'от' не может превышать зарплату 'до'";
            }
        }

        // 6. Description validation
        if (descriptionArea != null && descriptionArea.getText() != null) {
            if (descriptionArea.getText().length() > DESCRIPTION_MAX_LENGTH) {
                valid = false;
                setFieldError(descriptionArea, descriptionErrorLabel, "Описание превышает лимит в " + DESCRIPTION_MAX_LENGTH + " символов");
                if (firstErrorMessage == null) firstErrorMessage = "Описание вакансии слишком длинное";
            } else {
                clearFieldError(descriptionArea, descriptionErrorLabel);
            }
        }

        // 7. General error banner
        if (errorLabel != null) {
            if (!valid && userInteracted && firstErrorMessage != null) {
                errorLabel.setText(firstErrorMessage);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            } else {
                errorLabel.setText("");
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }
        }

        // 8. Bind dynamic state of OK button
        if (okButton != null) {
            okButton.setDisable(!valid);
        }

        return valid;
    }

    private void updateCharacterCounters() {
        if (titleCountLabel != null && titleField != null) {
            int len = titleField.getText() != null ? titleField.getText().length() : 0;
            titleCountLabel.setText(len + "/" + TITLE_MAX_LENGTH);
            if (len > TITLE_MAX_LENGTH) {
                titleCountLabel.setStyle("-fx-text-fill: #c73545; -fx-font-weight: bold;");
            } else {
                titleCountLabel.setStyle("");
            }
        }

        if (descriptionCountLabel != null && descriptionArea != null) {
            int len = descriptionArea.getText() != null ? descriptionArea.getText().length() : 0;
            descriptionCountLabel.setText(len + "/" + DESCRIPTION_MAX_LENGTH);
            if (len > DESCRIPTION_MAX_LENGTH) {
                descriptionCountLabel.setStyle("-fx-text-fill: #c73545; -fx-font-weight: bold;");
            } else {
                descriptionCountLabel.setStyle("");
            }
        }
    }

    private void setFieldError(Control control, Label label, String message) {
        setControlErrorStyle(control, true);
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void clearFieldError(Control control, Label label) {
        setControlErrorStyle(control, false);
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void setControlErrorStyle(Control control, boolean isError) {
        if (control == null) return;
        control.getStyleClass().removeAll("input-error");
        if (isError) {
            control.getStyleClass().add("input-error");
        }
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ₽";
        return CURRENCY_FORMAT.format(amount) + " ₽";
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

