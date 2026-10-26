package org.example.aav.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.example.aav.model.UserAccount;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u where u.username=:username")
  Optional<UserAccount> lockByUsername(@Param("username") String username);
}
