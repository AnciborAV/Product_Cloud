package org.example.aav.repository;

import java.util.*;
import org.example.aav.model.Review;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

/** Этап 4: создаём операции поиска и удаления отзывов; реализацию предоставит Spring Data JPA. Учебная последовательность. */
public interface ReviewRepository extends JpaRepository<Review, UUID> {
  /**
   * Загружает страницу отзывов от новых к старым вместе с авторами и товарами.
   * @param pageable номер и размер страницы, а также порядок сортировки
   */
  @EntityGraph(attributePaths = {"user", "product"})
  Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

  /**
   * Загружает записи пользователя от новых к старым.
   * @param username логин пользователя для входа
   */
  @EntityGraph(attributePaths = {"user", "product"})
  List<Review> findByUserUsernameOrderByCreatedAtDesc(String username);

  /**
   * Ищет отзыв конкретного пользователя о конкретном товаре.
   * @param userId уникальный номер пользователя
   * @param productId уникальный номер товара
   */
  Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);

  /**
   * Удаляет запись по номеру только у указанного владельца.
   * @param id уникальный номер записи
   * @param userId уникальный номер пользователя
   */
  void deleteByIdAndUserId(UUID id, UUID userId);
}
