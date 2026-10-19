package ru.aav.order;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.aav.account.*;
import ru.aav.cart.*;
import ru.aav.repository.ProductRepository;

/** Оформление атомарно: сохраняем снимок заказа и очищаем только корзину его владельца. */
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final AccountService accounts;
    private final CartService carts;
    private final CartRepository cart;
    private final OrderRepository orders;
    private final ProductRepository products;
    public OrderService(AccountService accounts, CartService carts, CartRepository cart, OrderRepository orders, ProductRepository products) {
        this.accounts = accounts; this.carts = carts; this.cart = cart; this.orders = orders; this.products = products;
    }
    public Page<OrderView> history(String email, int page) {
        if (page < 0 || page > 1000000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return orders.findByAccountIdOrderByCreatedAtDescIdDesc(accounts.require(email).getId(), PageRequest.of(page, 10)).map(OrderView::from);
    }
    public OrderView get(String email, long id) {
        return OrderView.from(orders.findByIdAndAccountId(id, accounts.require(email).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
    @Transactional
    public long checkout(String email, CheckoutForm form) {
        var a = carts.lock(email);
        // Тот же токен повторно возвращает тот же заказ, не оформляя дубликат.
        var previous = orders.findByCheckoutTokenAndAccountId(form.token(), a.getId());
        if (previous.isPresent()) return previous.get().getId();
        var items = cart.findByAccountIdOrderByIdAsc(a.getId());
        if (items.isEmpty()) throw new IllegalArgumentException("Корзина пуста. Сначала добавьте продукты");
        var order = orders.saveAndFlush(new CustomerOrder(a, items.stream().map(OrderLine::new).toList(),
                form.recipient(), form.phone(), form.address(), form.comment(), form.token()));
        cart.deleteByAccountId(a.getId());
        return order.getId();
    }
    @Transactional
    public void repeat(String email, long id) {
        var a = carts.lock(email);
        var order = orders.findByIdAndAccountId(id, a.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Сначала проверяем весь состав; при отсутствии продукта не добавляем половину заказа.
        for (var line : order.getLines()) {
            if (!products.existsById(line.getProductId())) throw new IllegalArgumentException("Один из продуктов больше недоступен в каталоге");
        }
        for (var line : order.getLines()) carts.add(email, line.getProductId(), line.getQuantity());
    }
}
