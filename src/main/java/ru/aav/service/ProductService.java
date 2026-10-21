package ru.aav.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ru.aav.account.AccountRepository;
import ru.aav.cart.CartItem;
import ru.aav.cart.CartRepository;
import ru.aav.model.Product;
import ru.aav.model.ProductView;
import ru.aav.repository.ProductRepository;

import java.math.BigDecimal;

/**
 * Сервис координирует операции с товарами и задаёт границы транзакций. Контроллер обращается к
 * сервису, а сервис — к репозиторию. @Service регистрирует компонент в Spring. readOnly=true задаёт
 * режим чтения по умолчанию; это подсказка инфраструктуре для оптимизаций, а не защита от записи.
 * Аннотации на изменяющих методах включают обычные транзакции. Транзакции применяются при вызове
 * через Spring-прокси, например из контроллера. {@code @Transactional} задаёт границу транзакции
 * при вызове через Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует уже открытую
 * транзакцию.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {
    // Ограничиваем объём одной выдачи, чтобы не загружать весь каталог в память.
    /** Максимальное количество продуктов на одной странице. */
    private static final int PAGE_SIZE = 20;

    /** Репозиторий, изолирующий хранение данных от вызывающего слоя. */
    private final ProductRepository repository;

    /** Зависимость для поиска и изменения учётных записей. */
    private final AccountRepository accounts;

    /** Зависимость для операций с личной корзиной. */
    private final CartRepository carts;

    /**
     * Внедрение зависимости через конструктор: Spring передаёт реализацию репозитория. final
     * фиксирует зависимость; единственному конструктору @Autowired не требуется.
     */
    public ProductService(
            ProductRepository repository, AccountRepository accounts, CartRepository carts) {
        this.repository = repository;
        this.accounts = accounts;
        this.carts = carts;
    }

    /**
     * Возвращает страницу из 20 товаров; номер страницы начинается с нуля. PageRequest задаёт
     * номер, размер и сортировку по уникальному id для стабильного порядка. Page содержит товары и
     * сведения об общем количестве/страницах; для общего количества Spring Data при необходимости
     * выполняет отдельный запрос COUNT. map(ProductView::from) преобразует элементы, сохраняя
     * метаданные страницы.
     */
    public Page<ProductView> getProducts(int page) {
        return repository
                .findAll(PageRequest.of(page, PAGE_SIZE, Sort.by("id")))
                .map(ProductView::from);
    }

    /** Возвращает снимок найденного товара; отсутствие записи не маскируется null. */
    public ProductView getProductById(int id) {
        return ProductView.from(requireProduct(id));
    }

    /**
     * Проверяет данные конструктором сущности и сохраняет новый товар. Транзакция объединяет работу
     * с БД в одну операцию; при необработанной RuntimeException изменения откатываются. save()
     * передаёт сущность ORM, а окончательная синхронизация с БД происходит при flush/завершении
     * транзакции. {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси;
     * readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public ProductView createProduct(String name, BigDecimal price) {
        return ProductView.from(repository.save(new Product(name, price)));
    }

    /**
     * Обновляет существующий товар. Полученная в транзакции сущность управляется Hibernate.
     * Повторный save() не нужен: механизм dirty checking обнаружит изменения полей и синхронизирует
     * их с базой при flush. var выводит тип Product на этапе компиляции; это не динамическая
     * типизация. {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси;
     * readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public ProductView updateProduct(int id, String name, BigDecimal price) {
        // Найденный или созданный продукт; изменения сущности отслеживает Hibernate в транзакции.
        var product = requireProduct(id);
        product.update(name, price);
        return ProductView.from(product);
    }

    /**
     * Удаляет найденный товар в транзакции. Сначала проверяет наличие, чтобы запрос на удаление
     * отсутствующего товара не выглядел успешным. {@code @Transactional} задаёт границу транзакции
     * при вызове через Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует уже открытую
     * транзакцию.
     */
    @Transactional
    public void deleteProduct(int id) {
        repository.delete(requireProduct(id));
    }

    /** Личный список не включает старые общие записи и продукты других пользователей. */
    public Page<ProductView> getPersonalProducts(long ownerId, int page) {
        if (page < 0 || page > 1_000_000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Некорректный номер страницы");
        }
        return repository
                .findByOwnerId(ownerId, PageRequest.of(page, PAGE_SIZE, Sort.by("id")))
                .map(ProductView::from);
    }

    /**
     * Один ввод сразу создаёт личный продукт и строку корзины в одной транзакции.
     * {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси; readOnly —
     * оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public ProductView createInCart(long ownerId, String name, BigDecimal price) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = accounts.findLockedById(ownerId).orElseThrow();
        // Найденный или созданный продукт; изменения сущности отслеживает Hibernate в транзакции.
        var product = repository.save(new Product(name, price, ownerId));
        carts.save(new CartItem(account, product, 1));
        return ProductView.from(product);
    }

    /**
     * Удаление из сохранённого списка также убирает позицию из своей корзины. История заказов
     * остаётся. {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси;
     * readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public void deletePersonal(long ownerId, int productId) {
        accounts.findLockedById(ownerId).orElseThrow();
        // Найденный или созданный продукт; изменения сущности отслеживает Hibernate в транзакции.
        var product =
                repository
                        .findByIdAndOwnerId(productId, ownerId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));
        carts.deleteByAccountIdAndProductId(ownerId, productId);
        carts.flush();
        repository.delete(product);
    }

    /**
     * Очищает весь личный список, включая все страницы. Чужие записи и старый общий каталог не
     * затрагивает. {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси;
     * readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public void clearPersonal(long ownerId) {
        accounts.findLockedById(ownerId).orElseThrow();
        carts.deleteByAccountIdAndProductOwnerId(ownerId, ownerId);
        carts.flush();
        repository.deleteAll(repository.findAllByOwnerId(ownerId));
    }

    /**
     * Общий поиск для чтения, обновления и удаления. Лямбда в orElseThrow создаёт исключение только
     * если Optional пуст. Метод использует транзакцию вызывающего метода сервиса.
     */
    private Product requireProduct(int id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
