package ru.aav.account;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Нормализованный email уникален. Блокировка аккаунта сериализует изменения его корзины. */
public interface AccountRepository extends JpaRepository<CustomerAccount, Long> {
    /**
     * Ищет аккаунт по нормализованному email; Optional явно обозначает возможное отсутствие записи.
     */
    Optional<CustomerAccount> findByEmail(String email);

    /**
     * Проверяет занятость email до регистрации; окончательную уникальность обеспечивает ограничение
     * базы.
     */
    boolean existsByEmail(String email);

    /**
     * Блокирует строку аккаунта до завершения транзакции, чтобы изменения одной корзины выполнялись
     * последовательно. {@code @Lock} запрашивает блокировку PESSIMISTIC_WRITE, удерживаемую до
     * завершения транзакции. {@code @Query} задаёт JPQL-запрос в терминах сущностей и их полей.
     * {@code @Param} связывает Java-аргумент с именованным параметром JPQL.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CustomerAccount a where a.id = :id")
    Optional<CustomerAccount> findLockedById(@Param("id") Long id);
}
