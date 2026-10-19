package ru.aav.cart;

import org.springframework.data.jpa.repository.*;
import java.util.*;

/** Любой поиск и удаление ограничены id владельца — чужую корзину запросить нельзя. */
public interface CartRepository extends JpaRepository<CartItem, Long> {
    @EntityGraph(attributePaths = "product")
    List<CartItem> findByAccountIdOrderByIdAsc(Long accountId);
    Optional<CartItem> findByAccountIdAndProductId(Long accountId, Integer productId);
    void deleteByAccountIdAndProductId(Long accountId, Integer productId);
    void deleteByAccountId(Long accountId);
}
