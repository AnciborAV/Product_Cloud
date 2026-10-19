package ru.aav.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aav.model.Product;

/**
 * Доступ к таблице товаров через Spring Data JPA.
 * Product — тип сущности, Integer — тип её идентификатора.
 * Spring создаёт реализацию интерфейса во время запуска: писать SQL для обычных
 * операций save, findById, findAll и delete здесь не требуется.
 * findById возвращает Optional: отсутствие записи обрабатывает сервис.
 * findAll(Pageable) получает страницу данных, а не весь каталог.
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {
    org.springframework.data.domain.Page<Product> findByOwnerId(Long ownerId, org.springframework.data.domain.Pageable pageable);
    java.util.List<Product> findAllByOwnerId(Long ownerId);
    java.util.Optional<Product> findByIdAndOwnerId(Integer id, Long ownerId);
}
