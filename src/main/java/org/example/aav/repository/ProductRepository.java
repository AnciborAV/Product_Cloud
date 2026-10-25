package org.example.aav.repository;

import java.util.UUID;
import org.example.aav.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** Этап 4: создаём доступ к таблице товаров; реализацию интерфейса предоставит Spring Data JPA. Учебная последовательность. */
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
  /**
   * Ищет страницу товаров по названию и необязательной категории в базе.
   * @param query текст поиска товара
   * @param category категория товара
   * @param pageable номер и размер страницы, а также порядок сортировки
   */
  @Query(
      "select p from ProductEntity p where (:category is null or p.category = :category) and p.searchName like concat('%', :query, '%') escape '!'")
  Page<ProductEntity> search(String query, Category category, Pageable pageable);
}
