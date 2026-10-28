package ru.mirea.hrsystem.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class UiUtils {
    private UiUtils() {}

    public static void showErrorAlert(String title, String message) {
        showErrorAlert(findActiveWindow(), title, message);
    }

    public static void showErrorAlert(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        setupAlert(alert, owner, "Ошибка", title);
        alert.showAndWait();
    }

    public static void showInfoAlert(String title, String message) {
        showInfoAlert(findActiveWindow(), title, message);
    }

    public static void showInfoAlert(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        setupAlert(alert, owner, "Информация", title);
        alert.showAndWait();
    }

    public static boolean showConfirmDialog(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        setupAlert(alert, owner, "Подтверждение", "Требуется подтверждение операции");
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static void setupAlert(Alert alert, Window owner, String title, String headerText) {
        Window targetOwner = owner != null ? owner : findActiveWindow();
        if (targetOwner != null) {
            alert.initOwner(targetOwner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.setTitle(title);
        alert.setHeaderText(headerText);

        try {
            var css = UiUtils.class.getResource("/ru/mirea/hrsystem/css/app.css");
            if (css != null && alert.getDialogPane() != null) {
                alert.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }

    public static Window findActiveWindow() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);
    }
}

