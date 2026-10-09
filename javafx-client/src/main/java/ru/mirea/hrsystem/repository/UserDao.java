package ru.mirea.hrsystem.repository;

import ru.mirea.hrsystem.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    List<User> findAllEmployers();
    Optional<User> findById(Long id);
}
