package ru.aav.order;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Страница истории и отдельный заказ всегда запрашиваются с ограничением по владельцу. */
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    /**
     * Читает страницу заказов владельца; дополнительная сортировка по id устраняет неоднозначность
     * одинаковых дат.
     */
    Page<CustomerOrder> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId, Pageable page);

    /** Находит заказ по номеру и владельцу одновременно. */
    Optional<CustomerOrder> findByIdAndAccountId(Long id, Long accountId);

    /**
     * Находит результат уже выполненного оформления, чтобы повтор HTTP-запроса не создавал второй
     * заказ.
     */
    Optional<CustomerOrder> findByCheckoutTokenAndAccountId(String token, Long accountId);
}
