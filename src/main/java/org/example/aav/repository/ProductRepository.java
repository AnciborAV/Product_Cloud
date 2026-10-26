package org.example.aav.repository;

import java.util.UUID;
import org.example.aav.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
  @Query(
      "select p from ProductEntity p where (:category is null or p.category = :category) and p.searchName like concat('%', :query, '%') escape '!'")
  Page<ProductEntity> search(String query, Category category, Pageable pageable);
}
