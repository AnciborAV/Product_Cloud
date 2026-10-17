package ru.aav.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aav.model.Product;
import ru.aav.model.ProductView;
import ru.aav.repository.ProductRepository;

import java.math.BigDecimal;

/**
 * Сервис координирует операции с товарами и задаёт границы транзакций.
 * Контроллер обращается к сервису, а сервис — к репозиторию.
 * @Service регистрирует компонент в Spring. readOnly=true задаёт режим чтения
 * по умолчанию; это подсказка инфраструктуре для оптимизаций, а не защита от записи.
 * Аннотации на изменяющих методах включают обычные транзакции.
 * Транзакции применяются при вызове через Spring-прокси, например из контроллера.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {
    // Ограничиваем объём одной выдачи, чтобы не загружать весь каталог в память.
    private static final int PAGE_SIZE = 20;
    private final ProductRepository repository;

    /**
     * Внедрение зависимости через конструктор: Spring передаёт реализацию репозитория.
     * final фиксирует зависимость; единственному конструктору @Autowired не требуется.
     */
    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Возвращает страницу из 20 товаров; номер страницы начинается с нуля.
     * PageRequest задаёт номер, размер и сортировку по уникальному id для стабильного порядка.
     * Page содержит товары и сведения об общем количестве/страницах; для общего
     * количества Spring Data при необходимости выполняет отдельный запрос COUNT.
     * map(ProductView::from) преобразует элементы, сохраняя метаданные страницы.
     */
    public Page<ProductView> getProducts(int page) {
        return repository.findAll(PageRequest.of(page, PAGE_SIZE, Sort.by("id")))
                .map(ProductView::from);
    }

    /**
     * Возвращает снимок найденного товара; отсутствие записи не маскируется null.
     */
    public ProductView getProductById(int id) {
        return ProductView.from(requireProduct(id));
    }

    /**
     * Проверяет данные конструктором сущности и сохраняет новый товар.
     * Транзакция объединяет работу с БД в одну операцию; при необработанной
     * RuntimeException изменения откатываются. save() передаёт сущность ORM,
     * а окончательная синхронизация с БД происходит при flush/завершении транзакции.
     */
    @Transactional
    public ProductView createProduct(String name, BigDecimal price) {
        return ProductView.from(repository.save(new Product(name, price)));
    }

    /**
     * Обновляет существующий товар. Полученная в транзакции сущность управляется Hibernate.
     * Повторный save() не нужен: механизм dirty checking обнаружит изменения полей
     * и синхронизирует их с базой при flush. var выводит тип Product на этапе компиляции;
     * это не динамическая типизация.
     */
    @Transactional
    public ProductView updateProduct(int id, String name, BigDecimal price) {
        var product = requireProduct(id);
        product.update(name, price);
        return ProductView.from(product);
    }

    /**
     * Удаляет найденный товар в транзакции. Сначала проверяет наличие,
     * чтобы запрос на удаление отсутствующего товара не выглядел успешным.
     */
    @Transactional
    public void deleteProduct(int id) {
        repository.delete(requireProduct(id));
    }

    /**
     * Общий поиск для чтения, обновления и удаления.
     * Лямбда в orElseThrow создаёт исключение только если Optional пуст.
     * Метод использует транзакцию вызывающего метода сервиса.
     */
    private Product requireProduct(int id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
