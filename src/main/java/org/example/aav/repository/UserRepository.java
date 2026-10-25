package org.example.aav.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.example.aav.model.UserAccount;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Этап 4: создаём поиск и блокировку аккаунтов в базе; реализацию предоставит Spring Data JPA. Учебная последовательность. */
public interface UserRepository extends JpaRepository<UserAccount, UUID> {
  /**
   * Ищет аккаунт по логину; Optional позволяет явно обработать отсутствие пользователя.
   * @param username логин пользователя для входа
   */
  Optional<UserAccount> findByUsername(String username);

  /**
   * Ищет аккаунт и блокирует его запись на время изменения в транзакции.
   * @param username логин пользователя для входа
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u where u.username=:username")
  Optional<UserAccount> lockByUsername(@Param("username") String username);
}
