package ru.mirea.hrsystem.repository;

import ru.mirea.hrsystem.model.Vacancy;
import java.util.List;

/**
 * ============================================================================
 * ПРАКТИКА ПО ТЕМЕ: ДОСТУП К ДАННЫМ (JDBC)
 * ИСПОЛНИТЕЛЬ: Максим (Задача T9)
 * ============================================================================
 * 
 * Реализовать сохранение и получение вакансий из PostgreSQL.
 * 
 * В работе пригодится понимание:
 * - JDBC, PreparedStatement, ResultSet.
 * - SQL запросы SELECT, INSERT, UPDATE.
 * 
 * Если нужна теория — она в самом конце файла.
 * 
 * ----------------------------------------------------------------------------
 * ШПАРГАЛКА (Код из преподского проекта Student Hub):
 * ----------------------------------------------------------------------------
 * /*
 *     // На основе прямого JDBC, который вы используете в архитектуре КР2
 *     public List<Student> findAll() {
 *         List<Student> list = new ArrayList<>();
 *         try (Connection c = DatabaseManager.openConnection();
 *              PreparedStatement ps = c.prepareStatement("SELECT * FROM students");
 *              ResultSet rs = ps.executeQuery()) {
 *             while (rs.next()) {
 *                 list.add(new Student(
 *                     rs.getLong("id"), 
 *                     rs.getString("first_name"), 
 *                     rs.getString("last_name")
 *                 ));
 *             }
 *         } catch (SQLException e) { ... }
 *         return list;
 *     }
 * *\/
 */
public class PostgresVacancyDao implements VacancyDao {
    @Override
    public List<Vacancy> findAll() {
        // TODO: Написать SQL запрос
        throw new UnsupportedOperationException("Метод не реализован (Задача Максима)");
    }
}
/*
 * ============================================================================
 * ТЕОРИЯ: JDBC и PreparedStatement
 * ============================================================================
 * 1. JDBC — низкоуровневый API в Java для работы с реляционными БД. Он дает полный 
 * контроль над SQL, но ответственность за закрытие ресурсов и обработку ошибок остается 
 * на разработчике (поэтому используем try-with-resources).
 * 
 * 2. PreparedStatement — это подготовленный SQL-запрос. В отличие от обычного Statement,
 * данные в него передаются через параметры (знак ?). Это критически важно, так как:
 * - данные не интерпретируются как часть SQL;
 * - снижается риск SQL-инъекций;
 * - корректно передаются даты, числа и null;
 * - план запроса в базе данных может переиспользоваться.
 * 
 * 3. ResultSet — это объект, хранящий результат SQL-запроса (строки из таблицы). 
 * Метод rs.next() сдвигает курсор на следующую строку.
 */
