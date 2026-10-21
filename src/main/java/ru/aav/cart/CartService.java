package ru.aav.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.aav.account.*;
import ru.aav.repository.ProductRepository;
import ru.aav.service.ProductNotFoundException;

/**
 * Изменения одной корзины сериализованы блокировкой аккаунта, включая оформление заказа.
 * {@code @Service} регистрирует сервис как компонент Spring. {@code @Transactional} задаёт границу
 * транзакции при вызове через Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует уже
 * открытую транзакцию.
 */
@Service
@Transactional(readOnly = true)
public class CartService {
    /** Зависимость для поиска и изменения учётных записей. */
    private final AccountService accounts;

    /** Репозиторий аккаунтов для операций хранения и блокировки владельца. */
    private final AccountRepository accountRepository;

    /** Репозиторий позиций корзины. */
    private final CartRepository cart;

    /** Зависимость для доступа к продуктам. */
    private final ProductRepository products;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public CartService(
            AccountService accounts,
            AccountRepository accountRepository,
            CartRepository cart,
            ProductRepository products) {
        this.accounts = accounts;
        this.accountRepository = accountRepository;
        this.cart = cart;
        this.products = products;
    }

    /**
     * Загружает позиции текущего владельца и вычисляет неизменяемый снимок корзины с точной суммой.
     */
    public CartView view(String email) {
        return CartView.from(cart.findByAccountIdOrderByIdAsc(accounts.require(email).getId()));
    }

    /**
     * Выполняет агрегатный запрос количества: для шапки не нужно загружать все продукты и
     * рассчитывать суммы.
     */
    public long quantity(String email) {
        return cart.sumQuantityByAccountId(accounts.require(email).getId());
    }

    /**
     * Получает блокировку аккаунта в уже открытой транзакции записи; её удержание защищает
     * изменение корзины и оформление заказа. {@code @Transactional} задаёт границу транзакции при
     * вызове через Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует уже открытую
     * транзакцию.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CustomerAccount lock(String email) {
        return accountRepository.findLockedById(accounts.require(email).getId()).orElseThrow();
    }

    /**
     * Проверяет количество и владельца продукта, затем увеличивает существующую позицию или создаёт
     * новую. {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси;
     * readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public void add(String email, int productId, int quantity) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = lock(email);
        if (quantity < 1 || quantity > 99)
            throw new IllegalArgumentException("Количество должно быть от 1 до 99");
        // Найденный или созданный продукт; изменения сущности отслеживает Hibernate в транзакции.
        var product =
                products.findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));
        if (product.getOwnerId() != null && !product.getOwnerId().equals(account.getId()))
            throw new ProductNotFoundException(productId);
        // Позиция корзины либо результат её поиска; изменение количества выполняется только для
        // владельца.
        var line = cart.findByAccountIdAndProductId(account.getId(), productId);
        if (line.isPresent()) line.get().setQuantity(line.get().getQuantity() + quantity);
        else cart.save(new CartItem(account, product, quantity));
    }

    /**
     * Меняет количество только в позиции корзины текущего владельца; отсутствие позиции сообщает
     * как ошибку. {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси;
     * readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public void update(String email, int productId, int quantity) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = lock(email);
        // Позиция корзины либо результат её поиска; изменение количества выполняется только для
        // владельца.
        var line =
                cart.findByAccountIdAndProductId(account.getId(), productId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Продукт уже удалён из корзины"));
        line.setQuantity(quantity);
    }

    /**
     * Удаляет одну позицию под блокировкой аккаунта. {@code @Transactional} задаёт границу
     * транзакции при вызове через Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует
     * уже открытую транзакцию.
     */
    @Transactional
    public void remove(String email, int productId) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = lock(email);
        cart.deleteByAccountIdAndProductId(account.getId(), productId);
    }

    /**
     * Удаляет все позиции текущей корзины под блокировкой аккаунта. {@code @Transactional} задаёт
     * границу транзакции при вызове через Spring-прокси; readOnly — оптимизация чтения, MANDATORY
     * требует уже открытую транзакцию.
     */
    @Transactional
    public void clear(String email) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = lock(email);
        cart.deleteByAccountId(account.getId());
    }
}
