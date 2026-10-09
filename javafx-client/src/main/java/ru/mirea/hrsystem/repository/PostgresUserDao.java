package ru.mirea.hrsystem.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.exception.DatabaseException;
import ru.mirea.hrsystem.model.User;
import ru.mirea.hrsystem.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresUserDao implements UserDao {
    private static final Logger log = LoggerFactory.getLogger(PostgresUserDao.class);
    private final DatabaseManager dbManager;

    public PostgresUserDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public List<User> findAllEmployers() {
        String sql = "SELECT id, email, full_name, role, company_name FROM users WHERE role = 'EMPLOYER' ORDER BY company_name";
        List<User> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new User(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("company_name")
                ));
            }
            return list;
        } catch (SQLException e) {
            log.error("Ошибка при получении списка работодателей", e);
            throw new DatabaseException("Не удалось загрузить работодателей", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, email, full_name, role, company_name FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("full_name"),
                            rs.getString("role"),
                            rs.getString("company_name")
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка чтения пользователя", e);
        }
    }
}
