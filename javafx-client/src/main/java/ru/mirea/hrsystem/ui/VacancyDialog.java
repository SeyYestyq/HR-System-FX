package ru.mirea.hrsystem.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.model.Vacancy;

import java.io.IOException;
import java.util.List;

public final class VacancyDialog {
    private VacancyDialog() {
    }

    public static Vacancy show(Window owner, Vacancy vacancy, List<User> employers) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    VacancyDialog.class.getResource("/ru/mirea/hrsystem/view/VacancyDialog.fxml"));
            Parent root = loader.load();
            VacancyDialogController controller = loader.getController();
            controller.setData(vacancy, employers);

            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.APPLICATION_MODAL);
            }
            stage.setTitle(vacancy == null || vacancy.getId() == null ? "Новая вакансия" : "Редактирование вакансии");
            Scene scene = new Scene(root);
            if (owner != null && owner.getScene() != null) {
                scene.getStylesheets().addAll(owner.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.showAndWait();
            return controller.getResult();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось открыть форму вакансии", exception);
        }
    }
}
