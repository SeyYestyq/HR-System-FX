package ru.mirea.hrsystem.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.model.Vacancy;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class VacancyDialog {
    private static final Logger log = LoggerFactory.getLogger(VacancyDialog.class);

    public static Optional<Vacancy> show(Window owner, Vacancy existingVacancy, List<User> employers) {
        Dialog<Vacancy> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existingVacancy != null ? "Редактирование вакансии" : "Новая вакансия");

        try {
            FXMLLoader loader = new FXMLLoader(VacancyDialog.class.getResource("/ru/mirea/hrsystem/view/VacancyDialog.fxml"));
            VBox content = loader.load();

            VacancyDialogController controller = loader.getController();
            controller.setEmployers(employers);
            controller.setVacancy(existingVacancy);

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(content);
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            String cssPath = VacancyDialog.class.getResource("/ru/mirea/hrsystem/css/app.css") != null
                    ? VacancyDialog.class.getResource("/ru/mirea/hrsystem/css/app.css").toExternalForm()
                    : null;
            if (cssPath != null) {
                dialogPane.getStylesheets().add(cssPath);
            }

            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.setText(existingVacancy != null ? "Сохранить" : "Создать");
                okButton.getStyleClass().add("primary-button");

                controller.setOkButton(okButton);

                okButton.addEventFilter(ActionEvent.ACTION, event -> {
                    if (!controller.validate()) {
                        event.consume();
                    }
                });
            }

            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            if (cancelButton != null) {
                cancelButton.setText("Отмена");
                cancelButton.getStyleClass().add("secondary-button");
            }

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    return controller.buildVacancy();
                }
                return null;
            });

            return dialog.showAndWait();
        } catch (IOException e) {
            log.error("Ошибка загрузки формы VacancyDialog.fxml", e);
            throw new IllegalStateException("Не удалось открыть форму вакансии", e);
        }
    }
}
