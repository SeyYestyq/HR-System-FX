package ru.mirea.hrsystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.hrsystem.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    Optional<UserAccount> findByUuid(UUID uuid);
    boolean existsByEmailIgnoreCase(String email);
    Page<UserAccount> findAll(Pageable pageable);
}
