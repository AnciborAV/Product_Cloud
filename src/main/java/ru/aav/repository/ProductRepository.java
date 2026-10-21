package ru.aav.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.aav.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Доступ к таблице товаров через Spring Data JPA. Product — тип сущности, Integer — тип её
 * идентификатора. Spring создаёт реализацию интерфейса во время запуска: писать SQL для обычных
 * операций save, findById, findAll и delete здесь не требуется. findById возвращает Optional:
 * отсутствие записи обрабатывает сервис. findAll(Pageable) получает страницу данных, а не весь
 * каталог.
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {
    /** Получает страницу личного списка; общие записи и чужие продукты не попадают в результат. */
    Page<Product> findByOwnerId(Long ownerId, Pageable pageable);

    /** Получает весь список одного владельца для полной очистки, независимо от текущей страницы. */
    List<Product> findAllByOwnerId(Long ownerId);

    /** Ищет продукт одновременно по id и владельцу, не раскрывая наличие чужой записи. */
    Optional<Product> findByIdAndOwnerId(Integer id, Long ownerId);
}
