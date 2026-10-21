package ru.mirea.hrsystem.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.exception.DatabaseException;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresVacancyDao implements VacancyDao {
    private static final Logger log = LoggerFactory.getLogger(PostgresVacancyDao.class);

    private static final String SQL_FIND_ALL =
            "SELECT v.id, v.title, v.description, v.salary_min, v.salary_max, v.status, " +
            "       v.employer_id, v.created_at, v.updated_at, " +
            "       u.company_name, u.email, u.full_name " +
            "FROM vacancies v " +
            "JOIN users u ON v.employer_id = u.id " +
            "ORDER BY v.id DESC";

    private static final String SQL_FIND_BY_ID =
            "SELECT v.id, v.title, v.description, v.salary_min, v.salary_max, v.status, " +
            "       v.employer_id, v.created_at, v.updated_at, " +
            "       u.company_name, u.email, u.full_name " +
            "FROM vacancies v " +
            "JOIN users u ON v.employer_id = u.id " +
            "WHERE v.id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO vacancies (title, description, salary_min, salary_max, status, employer_id, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

    private static final String SQL_UPDATE =
            "UPDATE vacancies " +
            "SET title = ?, description = ?, salary_min = ?, salary_max = ?, status = ?, employer_id = ?, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM vacancies WHERE id = ?";

    private final DatabaseManager dbManager;

    public PostgresVacancyDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public List<Vacancy> findAll() {
        List<Vacancy> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToVacancy(rs));
            }
            return list;
        } catch (SQLException e) {
            log.error("Ошибка при получении списка вакансий из БД", e);
            throw new DatabaseException("Не удалось загрузить список вакансий", e);
        }
    }

    @Override
    public Optional<Vacancy> findById(Long id) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToVacancy(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Ошибка при поиске вакансии ID={}", id, e);
            throw new DatabaseException("Не удалось найти вакансию с ID=" + id, e);
        }
    }

    @Override
    public Vacancy create(Vacancy vacancy) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, vacancy.getTitle());
            ps.setString(2, vacancy.getDescription());
            ps.setBigDecimal(3, vacancy.getSalaryMin());
            ps.setBigDecimal(4, vacancy.getSalaryMax());
            ps.setString(5, vacancy.getStatus() != null ? vacancy.getStatus().name() : VacancyStatus.ACTIVE.name());
            ps.setLong(6, vacancy.getEmployerId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Создание вакансии не выполнено: ни одна строка не добавлена");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    vacancy.setId(generatedKeys.getLong(1));
                } else {
                    throw new DatabaseException("Создание вакансии не выполнено: ID не сгенерирован");
                }
            }

            vacancy.setCreatedAt(LocalDateTime.now());
            vacancy.setUpdatedAt(LocalDateTime.now());
            return vacancy;
        } catch (SQLException e) {
            log.error("Ошибка при создании вакансии '{}'", vacancy.getTitle(), e);
            throw new DatabaseException("Ошибка сохранения вакансии в базу данных", e);
        }
    }

    @Override
    public void update(Vacancy vacancy) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, vacancy.getTitle());
            ps.setString(2, vacancy.getDescription());
            ps.setBigDecimal(3, vacancy.getSalaryMin());
            ps.setBigDecimal(4, vacancy.getSalaryMax());
            ps.setString(5, vacancy.getStatus() != null ? vacancy.getStatus().name() : VacancyStatus.ACTIVE.name());
            ps.setLong(6, vacancy.getEmployerId());
            ps.setLong(7, vacancy.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Вакансия с ID=" + vacancy.getId() + " не найдена для обновления");
            }
            vacancy.setUpdatedAt(LocalDateTime.now());
        } catch (SQLException e) {
            log.error("Ошибка при обновлении вакансии ID={}", vacancy.getId(), e);
            throw new DatabaseException("Ошибка обновления вакансии в базе данных", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setLong(1, id);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Вакансия с ID=" + id + " не найдена для удаления");
            }
        } catch (SQLException e) {
            log.error("Ошибка при удалении вакансии ID={}", id, e);
            throw new DatabaseException("Ошибка удаления вакансии из базы данных", e);
        }
    }

    private Vacancy mapRowToVacancy(ResultSet rs) throws SQLException {
        Vacancy v = new Vacancy();
        v.setId(rs.getLong("id"));
        v.setTitle(rs.getString("title"));
        v.setDescription(rs.getString("description"));
        v.setSalaryMin(rs.getBigDecimal("salary_min"));
        v.setSalaryMax(rs.getBigDecimal("salary_max"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                v.setStatus(VacancyStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                v.setStatus(VacancyStatus.ACTIVE);
            }
        } else {
            v.setStatus(VacancyStatus.ACTIVE);
        }

        v.setEmployerId(rs.getLong("employer_id"));

        String company = rs.getString("company_name");
        if (company == null || company.isBlank()) {
            company = rs.getString("full_name");
        }
        if (company == null || company.isBlank()) {
            company = rs.getString("email");
        }
        v.setEmployerCompanyName(company);

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            v.setCreatedAt(created.toLocalDateTime());
        }

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            v.setUpdatedAt(updated.toLocalDateTime());
        }

        return v;
    }
}
