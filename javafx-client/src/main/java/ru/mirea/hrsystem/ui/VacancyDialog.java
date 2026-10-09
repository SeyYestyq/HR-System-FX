package ru.mirea.hrsystem.ui;

import ru.mirea.hrsystem.model.Vacancy;
import javafx.stage.Window;
import java.util.List;

/**
 * ============================================================================
 * ПРАКТИКА ПО ТЕМЕ: JAVAFX СЦЕНЫ И FXML
 * ИСПОЛНИТЕЛЬ: Дамир (Задача T8)
 * ============================================================================
 * 
 * Тебе нужно написать обертку для вызова окна редактирования вакансии.
 * 
 * ----------------------------------------------------------------------------
 * КАК ДОЛЖНА РАБОТАТЬ ПРОГРАММА:
 * ----------------------------------------------------------------------------
 * 1. Использовать `FXMLLoader` для загрузки файла `VacancyDialog.fxml`.
 * 2. Получить контроллер (`VacancyDialogController`) из лоадера.
 * 3. Передать в контроллер нужные данные (вакансию для редактирования и список работодателей).
 * 4. Создать `Stage`, назначить `Scene` и вызвать `showAndWait()`.
 * 
 * Если нужна теория — она в самом конце файла.
 * 
 * ----------------------------------------------------------------------------
 * ШПАРГАЛКА (Код из преподского проекта Student Hub):
 * ----------------------------------------------------------------------------
 * /*
 *     // На основе ru.mirea.studenthub.fx.ui.StudentDialog
 *     public static void show(Window owner, Student student) {
 *         FXMLLoader loader = new FXMLLoader(StudentDialog.class.getResource("StudentDialog.fxml"));
 *         Stage stage = new Stage();
 *         stage.setScene(new Scene(loader.load()));
 *         StudentDialogController controller = loader.getController();
 *         controller.setStudent(student);
 *         stage.showAndWait();
 *     }
 * *\/
 */
public class VacancyDialog {
    public static void show(Window owner, Vacancy vacancy, List<Object> employers) {
        // TODO: Реализовать загрузку FXML и открытие модального окна.
        throw new UnsupportedOperationException("Метод не реализован (Задача Дамира)");
    }
}
/*
 * ============================================================================
 * ТЕОРИЯ: FXMLLoader и Stage
 * ============================================================================
 * 1. Stage — это само окно приложения (рамка, кнопки закрыть/свернуть).
 * 2. Scene — это содержимое окна (холст, на котором располагаются кнопки и поля).
 * 3. FXMLLoader — утилита, которая парсит XML-разметку (FXML) и создает из нее 
 *    дерево реальных Java-объектов (Node), а также инстанцирует Controller.
 */
