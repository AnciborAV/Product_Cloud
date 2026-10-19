package ru.aav.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aav.account.*;
import ru.aav.repository.ProductRepository;
import ru.aav.service.ProductNotFoundException;

/** Изменения одной корзины сериализованы блокировкой аккаунта, включая оформление заказа. */
@Service
@Transactional(readOnly = true)
public class CartService {
    private final AccountService accounts;
    private final AccountRepository accountRepository;
    private final CartRepository cart;
    private final ProductRepository products;
    public CartService(AccountService accounts, AccountRepository accountRepository, CartRepository cart, ProductRepository products) {
        this.accounts = accounts; this.accountRepository = accountRepository; this.cart = cart; this.products = products;
    }
    public CartView view(String email) { return CartView.from(cart.findByAccountIdOrderByIdAsc(accounts.require(email).getId())); }
    public CustomerAccount lock(String email) {
        return accountRepository.findLockedById(accounts.require(email).getId()).orElseThrow();
    }
    @Transactional
    public void add(String email, int productId, int quantity) {
        var a = lock(email);
        if (quantity < 1 || quantity > 99) throw new IllegalArgumentException("Количество должно быть от 1 до 99");
        var p = products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        if (p.getOwnerId() != null && !p.getOwnerId().equals(a.getId()))
            throw new ProductNotFoundException(productId);
        var line = cart.findByAccountIdAndProductId(a.getId(), productId);
        if (line.isPresent()) line.get().setQuantity(line.get().getQuantity() + quantity);
        else cart.save(new CartItem(a, p, quantity));
    }
    @Transactional
    public void update(String email, int productId, int quantity) {
        var a = lock(email);
        var line = cart.findByAccountIdAndProductId(a.getId(), productId)
                .orElseThrow(() -> new IllegalArgumentException("Продукт уже удалён из корзины"));
        line.setQuantity(quantity);
    }
    @Transactional
    public void remove(String email, int productId) { var a = lock(email); cart.deleteByAccountIdAndProductId(a.getId(), productId); }
    @Transactional
    public void clear(String email) { var a = lock(email); cart.deleteByAccountId(a.getId()); }
}
