package org.example.aav.repository;

import java.util.*;
import org.example.aav.model.SelectedProduct;
import org.springframework.data.jpa.repository.*;

/** Этап 4: создаём доступ к выбранным товарам; реализацию предоставит Spring Data JPA. Учебная последовательность. */
public interface SelectedProductRepository extends JpaRepository<SelectedProduct, UUID> {
  /**
   * Загружает записи пользователя от новых к старым.
   * @param username логин пользователя для входа
   */
  @EntityGraph(attributePaths = "product")
  List<SelectedProduct> findByUserUsernameOrderByCreatedAtDesc(String username);

  /**
   * Проверяет, добавлен ли товар в список этого пользователя.
   * @param userId уникальный номер пользователя
   * @param productId уникальный номер товара
   */
  boolean existsByUserIdAndProductId(UUID userId, UUID productId);

  /**
   * Удаляет товар из списка указанного пользователя.
   * @param userId уникальный номер пользователя
   * @param productId уникальный номер товара
   */
  void deleteByUserIdAndProductId(UUID userId, UUID productId);

  long deleteByUserIdAndProductIdIn(UUID userId, Collection<UUID> productIds);
}
