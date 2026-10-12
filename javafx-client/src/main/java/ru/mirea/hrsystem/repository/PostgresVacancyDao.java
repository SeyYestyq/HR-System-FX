package ru.mirea.hrsystem.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.exception.DatabaseException;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresVacancyDao implements VacancyDao {
    private static final Logger log = LoggerFactory.getLogger(PostgresVacancyDao.class);
    private static final String SELECT_SQL = """
            SELECT v.id, v.title, v.description, v.salary_min, v.salary_max, v.status,
                   v.employer_id, u.company_name, v.created_at, v.updated_at
            FROM vacancies v
            LEFT JOIN users u ON u.id = v.employer_id
            """;

    private final DatabaseManager databaseManager;

    public PostgresVacancyDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<Vacancy> findAll() {
        String sql = SELECT_SQL + " ORDER BY v.id";
        List<Vacancy> vacancies = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                vacancies.add(map(rows));
            }
            return vacancies;
        } catch (SQLException exception) {
            log.error("Не удалось прочитать вакансии", exception);
            throw new DatabaseException("Не удалось прочитать вакансии", exception);
        }
    }

    @Override
    public Optional<Vacancy> findById(Long id) {
        String sql = SELECT_SQL + " WHERE v.id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return Optional.of(map(rows));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось прочитать вакансию", exception);
        }
    }

    @Override
    public Vacancy create(Vacancy vacancy) {
        String sql = """
                INSERT INTO vacancies (title, description, salary_min, salary_max, status, employer_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, vacancy);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    vacancy.setId(keys.getLong(1));
                }
            }
            return vacancy;
        } catch (SQLException exception) {
            log.error("Не удалось создать вакансию", exception);
            throw new DatabaseException("Не удалось создать вакансию", exception);
        }
    }

    @Override
    public void update(Vacancy vacancy) {
        String sql = """
                UPDATE vacancies
                SET title = ?, description = ?, salary_min = ?, salary_max = ?, status = ?,
                    employer_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, vacancy);
            statement.setLong(7, vacancy.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            log.error("Не удалось обновить вакансию {}", vacancy.getId(), exception);
            throw new DatabaseException("Не удалось обновить вакансию", exception);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM vacancies WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            log.error("Не удалось удалить вакансию {}", id, exception);
            throw new DatabaseException("Не удалось удалить вакансию", exception);
        }
    }

    private static void bind(PreparedStatement statement, Vacancy vacancy) throws SQLException {
        statement.setString(1, vacancy.getTitle());
        statement.setString(2, vacancy.getDescription());
        setMoney(statement, 3, vacancy.getSalaryMin());
        setMoney(statement, 4, vacancy.getSalaryMax());
        statement.setString(5, vacancy.getStatus() == null ? VacancyStatus.ACTIVE.name() : vacancy.getStatus().name());
        statement.setLong(6, vacancy.getEmployerId());
    }

    private static void setMoney(PreparedStatement statement, int index, java.math.BigDecimal value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.NUMERIC);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    private static Vacancy map(ResultSet rows) throws SQLException {
        Vacancy vacancy = new Vacancy(
                rows.getLong("id"),
                rows.getString("title"),
                rows.getString("description"),
                rows.getBigDecimal("salary_min"),
                rows.getBigDecimal("salary_max"),
                VacancyStatus.valueOf(rows.getString("status")),
                rows.getLong("employer_id"));
        vacancy.setEmployerCompanyName(rows.getString("company_name"));
        if (rows.getTimestamp("created_at") != null) {
            vacancy.setCreatedAt(rows.getTimestamp("created_at").toLocalDateTime());
        }
        if (rows.getTimestamp("updated_at") != null) {
            vacancy.setUpdatedAt(rows.getTimestamp("updated_at").toLocalDateTime());
        }
        return vacancy;
    }
}
