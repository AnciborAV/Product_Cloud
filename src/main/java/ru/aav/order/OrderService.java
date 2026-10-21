package ru.aav.order;

import jakarta.validation.Valid;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import ru.aav.account.*;
import ru.aav.cart.*;
import ru.aav.repository.ProductRepository;

/**
 * Оформление атомарно: сохраняем снимок заказа и очищаем только корзину его владельца.
 * {@code @Validated} включает проверку аргументов публичных методов при обращении через
 * Spring-прокси. {@code @Service} регистрирует сервис как компонент Spring. {@code @Transactional}
 * задаёт границу транзакции при вызове через Spring-прокси; readOnly — оптимизация чтения,
 * MANDATORY требует уже открытую транзакцию.
 */
@Validated
@Service
@Transactional(readOnly = true)
public class OrderService {
    /** Зависимость для поиска и изменения учётных записей. */
    private final AccountService accounts;

    /** Зависимость для операций с личной корзиной. */
    private final CartService carts;

    /** Репозиторий позиций корзины. */
    private final CartRepository cart;

    /** Зависимость для работы с заказами. */
    private final OrderRepository orders;

    /** Зависимость для доступа к продуктам. */
    private final ProductRepository products;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public OrderService(
            AccountService accounts,
            CartService carts,
            CartRepository cart,
            OrderRepository orders,
            ProductRepository products) {
        this.accounts = accounts;
        this.carts = carts;
        this.cart = cart;
        this.orders = orders;
        this.products = products;
    }

    /**
     * Возвращает страницу истории владельца в порядке от новых заказов к старым; ленивые строки
     * преобразуются в DTO внутри транзакции.
     */
    public Page<OrderView> history(String email, int page) {
        if (page < 0 || page > 1000000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return orders.findByAccountIdOrderByCreatedAtDescIdDesc(
                        accounts.require(email).getId(), PageRequest.of(page, 10))
                .map(OrderView::from);
    }

    /**
     * Получает один заказ только вместе с проверкой владельца; чужой или отсутствующий заказ даёт
     * HTTP 404.
     */
    public OrderView get(String email, long id) {
        return OrderView.from(
                orders.findByIdAndAccountId(id, accounts.require(email).getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    /**
     * Под блокировкой аккаунта сохраняет снимок состава и контактов и очищает корзину в одной
     * транзакции; повтор токена возвращает прежний заказ. {@code @Transactional} задаёт границу
     * транзакции при вызове через Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует
     * уже открытую транзакцию. {@code @Valid} запускает проверку ограничений полей переданного
     * объекта.
     */
    @Transactional
    public long checkout(String email, @Valid CheckoutForm form) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = carts.lock(email);
        // Тот же токен повторно возвращает тот же заказ, не оформляя дубликат.
        var previous = orders.findByCheckoutTokenAndAccountId(form.token(), account.getId());
        if (previous.isPresent()) return previous.get().getId();
        // Позиции, которые будут проверены и преобразованы в состав заказа.
        var items = cart.findByAccountIdOrderByIdAsc(account.getId());
        if (items.isEmpty())
            throw new IllegalArgumentException("Корзина пуста. Сначала добавьте продукты");
        // Созданный или найденный заказ; дальнейший ответ содержит его идентификатор или
        // представление.
        var order =
                orders.saveAndFlush(
                        new CustomerOrder(
                                account,
                                items.stream().map(OrderLine::new).toList(),
                                form.recipient(),
                                form.phone(),
                                form.address(),
                                form.comment(),
                                form.token()));
        cart.deleteByAccountId(account.getId());
        return order.getId();
    }

    /**
     * Проверяет доступность всего состава и добавляет позиции по текущим ценам; любая ошибка
     * откатывает все изменения корзины. {@code @Transactional} задаёт границу транзакции при вызове
     * через Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует уже открытую
     * транзакцию.
     */
    @Transactional
    public void repeat(String email, long id) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = carts.lock(email);
        // Созданный или найденный заказ; дальнейший ответ содержит его идентификатор или
        // представление.
        var order =
                orders.findByIdAndAccountId(id, account.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Сначала проверяем весь состав; при отсутствии продукта не добавляем половину заказа.
        for (var line : order.getLines()) {
            if (!products.existsById(line.getProductId()))
                throw new IllegalArgumentException(
                        "Один из продуктов больше недоступен в каталоге");
        }
        for (var line : order.getLines()) carts.add(email, line.getProductId(), line.getQuantity());
    }
}
