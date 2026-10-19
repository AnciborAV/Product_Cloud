package ru.aav.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** Нормализованный email уникален. Блокировка аккаунта сериализует изменения его корзины. */
public interface AccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByEmail(String email);
    boolean existsByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CustomerAccount a where a.id = :id")
    Optional<CustomerAccount> findLockedById(@Param("id") Long id);
}
