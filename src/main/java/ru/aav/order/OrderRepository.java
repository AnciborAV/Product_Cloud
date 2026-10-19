package ru.aav.order;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Страница истории и отдельный заказ всегда запрашиваются с ограничением по владельцу. */
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId, Pageable page);
    Optional<CustomerOrder> findByIdAndAccountId(Long id, Long accountId);
    Optional<CustomerOrder> findByCheckoutTokenAndAccountId(String token, Long accountId);
}
