package ru.mirea.hrsystem.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class UiUtils {
    private UiUtils() {}

    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Ошибка");
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Информация");
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static boolean showConfirmDialog(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.initOwner(owner);
        alert.setTitle("Подтверждение");
        alert.setHeaderText("Требуется подтверждение операции");
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
