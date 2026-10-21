package ru.aav.cart;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

/** Любой поиск и удаление ограничены id владельца — чужую корзину запросить нельзя. */
public interface CartRepository extends JpaRepository<CartItem, Long> {
    /**
     * Вычисляет суммарное количество продуктов владельца в базе; пустая корзина даёт ноль.
     * {@code @Query} задаёт JPQL-запрос в терминах сущностей и их полей. {@code @Param} связывает
     * Java-аргумент с именованным параметром JPQL.
     */
    @Query(
            "select coalesce(sum(item.quantity), 0) from CartItem item where item.account.id ="
                    + " :accountId")
    long sumQuantityByAccountId(@Param("accountId") Long accountId);

    /**
     * Загружает корзину одного владельца в стабильном порядке вместе с продуктами.
     * {@code @EntityGraph} указывает связанные сущности, которые надо загрузить вместе с
     * результатом, избегая отдельных запросов к каждому продукту.
     */
    @EntityGraph(attributePaths = "product")
    List<CartItem> findByAccountIdOrderByIdAsc(Long accountId);

    /** Ищет конкретную позицию только в корзине указанного владельца. */
    Optional<CartItem> findByAccountIdAndProductId(Long accountId, Integer productId);

    /** Удаляет конкретный продукт из корзины указанного владельца. */
    void deleteByAccountIdAndProductId(Long accountId, Integer productId);

    /** Удаляет все позиции только указанного аккаунта. */
    void deleteByAccountId(Long accountId);

    /** Удаляет из корзины позиции личного списка; старые общие товары не затрагивает. */
    void deleteByAccountIdAndProductOwnerId(Long accountId, Long ownerId);
}
