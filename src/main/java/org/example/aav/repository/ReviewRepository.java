package org.example.aav.repository;

import java.util.*;
import org.example.aav.model.Review;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
  @EntityGraph(attributePaths = {"user", "product"})
  Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @EntityGraph(attributePaths = {"user", "product"})
  List<Review> findByUserUsernameOrderByCreatedAtDesc(String username);

  Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);

  void deleteByIdAndUserId(UUID id, UUID userId);
}
