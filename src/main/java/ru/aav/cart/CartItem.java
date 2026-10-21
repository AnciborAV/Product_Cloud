package ru.aav.cart;

import jakarta.persistence.*;

import ru.aav.account.CustomerAccount;
import ru.aav.model.Product;

/**
 * В корзине хранится количество; актуальные название и цена читаются из каталога. {@code @Entity}
 * объявляет JPA-сущность, связанную со строкой таблицы. {@code @Table} задаёт таблицу и её
 * ограничения/индексы.
 */
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "product_id"}))
public class CartItem {
    /**
     * Идентификатор записи; объект получает его при сохранении. {@code @Id} помечает первичный
     * ключ. {@code @GeneratedValue} поручает генерацию идентификатора базе/ORM согласно выбранной
     * стратегии.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Учётная запись владельца этой записи. {@code @ManyToOne} задаёт связь с одной сущностью; LAZY
     * откладывает её загрузку, optional=false требует связь. {@code @JoinColumn} задаёт столбец
     * внешнего ключа.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private CustomerAccount account;

    /**
     * Продукт, на который ссылается позиция корзины. {@code @ManyToOne} задаёт связь с одной
     * сущностью; LAZY откладывает её загрузку, optional=false требует связь. {@code @JoinColumn}
     * задаёт столбец внешнего ключа.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Количество единиц продукта в позиции. {@code @Column} задаёт имя, допустимость NULL, длину
     * или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false)
    private int quantity;

    /** Конструктор для JPA: ORM создаёт экземпляр и заполняет поля при чтении из базы. */
    protected CartItem() {}

    /** Связывает владельца и продукт и проверяет допустимость количества. */
    public CartItem(CustomerAccount account, Product product, int quantity) {
        this.account = account;
        this.product = product;
        setQuantity(quantity);
    }

    /** Допускает только количество от 1 до 99; проверка действует и при прямом вызове Java-кода. */
    public void setQuantity(int quantity) {
        if (quantity < 1 || quantity > 99)
            throw new IllegalArgumentException("Количество должно быть от 1 до 99");
        this.quantity = quantity;
    }

    /** Возвращает связанный продукт; доступ к ленивой связи нужен внутри транзакции. */
    public Product getProduct() {
        return product;
    }

    /** Возвращает количество единиц. */
    public int getQuantity() {
        return quantity;
    }
}
