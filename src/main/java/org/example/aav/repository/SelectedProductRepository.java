package org.example.aav.repository;

import java.util.*;
import org.example.aav.model.SelectedProduct;
import org.springframework.data.jpa.repository.*;

public interface SelectedProductRepository extends JpaRepository<SelectedProduct, UUID> {
  @EntityGraph(attributePaths = "product")
  List<SelectedProduct> findByUserUsernameOrderByCreatedAtDesc(String username);

  boolean existsByUserIdAndProductId(UUID userId, UUID productId);

  void deleteByUserIdAndProductId(UUID userId, UUID productId);
}
