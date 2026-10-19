package ru.aav.cart;

import jakarta.persistence.*;
import ru.aav.account.CustomerAccount;
import ru.aav.model.Product;

/** В корзине хранится количество; актуальные название и цена читаются из каталога. */
@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "product_id"}))
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false)
    private CustomerAccount account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false) private int quantity;
    protected CartItem() { }
    public CartItem(CustomerAccount account, Product product, int quantity) {
        this.account = account; this.product = product; setQuantity(quantity);
    }
    public void setQuantity(int quantity) {
        if (quantity < 1 || quantity > 99) throw new IllegalArgumentException("Количество должно быть от 1 до 99");
        this.quantity = quantity;
    }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
}
