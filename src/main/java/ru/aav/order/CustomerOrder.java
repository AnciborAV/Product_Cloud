package ru.aav.order;

import jakarta.persistence.*;

import org.hibernate.annotations.BatchSize;

import ru.aav.account.CustomerAccount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Заказ хранит состав, цены и контакты на момент оформления. Оплаты в этом приложении нет.
 * {@code @Entity} объявляет JPA-сущность, связанную со строкой таблицы. {@code @Table} задаёт
 * таблицу и её ограничения/индексы.
 */
@Entity
@Table(name = "customer_orders", indexes = @Index(columnList = "account_id,created_at"))
public class CustomerOrder {
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
     * Момент создания записи в UTC. {@code @Column} задаёт имя, допустимость NULL, длину или
     * точность столбца; это не проверка HTML-формы.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Имя получателя на момент оформления. {@code @Column} задаёт имя, допустимость NULL, длину или
     * точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 80)
    private String recipient;

    /**
     * Контактный телефон. {@code @Column} задаёт имя, допустимость NULL, длину или точность
     * столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 40)
    private String phone;

    /**
     * Контактный адрес. {@code @Column} задаёт имя, допустимость NULL, длину или точность столбца;
     * это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 500)
    private String address;

    /**
     * Комментарий к заказу на момент оформления. {@code @Column} задаёт имя, допустимость NULL,
     * длину или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 1000)
    private String comment;

    /**
     * Уникальный токен оформления, предотвращающий дубли при повторе запроса. {@code @Column}
     * задаёт имя, допустимость NULL, длину или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 36, unique = true)
    private String checkoutToken;

    /**
     * Состав заказа или корзины; публичные DTO защищают список от изменения. {@code @BatchSize}
     * группирует загрузку ленивых коллекций; это уменьшает число запросов истории без JOIN FETCH с
     * пагинацией. {@code @ElementCollection} хранит значения строк заказа в отдельной таблице без
     * самостоятельных id. {@code @CollectionTable} задаёт таблицу значений и ссылку на заказ.
     * {@code @JoinColumn} задаёт столбец внешнего ключа. {@code @OrderColumn} сохраняет порядок
     * строк в базе.
     */
    @BatchSize(size = 10)
    @ElementCollection
    @CollectionTable(name = "customer_order_lines", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_number")
    private List<OrderLine> lines = new ArrayList<>();

    /** Конструктор для JPA: ORM создаёт экземпляр и заполняет поля при чтении из базы. */
    protected CustomerOrder() {}

    /**
     * Фиксирует состав, контакты, токен и время заказа; список копируется, чтобы внешние изменения
     * не меняли историю.
     */
    public CustomerOrder(
            CustomerAccount account,
            List<OrderLine> lines,
            String recipient,
            String phone,
            String address,
            String comment,
            String checkoutToken) {
        this.account = account;
        this.lines = new ArrayList<>(lines);
        this.createdAt = Instant.now();
        this.recipient = recipient.strip();
        this.phone = phone.strip();
        this.address = address.strip();
        this.comment = comment.strip();
        this.checkoutToken = checkoutToken;
    }

    /** Возвращает идентификатор записи. */
    public Long getId() {
        return id;
    }

    /** Возвращает момент создания записи. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Возвращает имя получателя из снимка заказа. */
    public String getRecipient() {
        return recipient;
    }

    /** Возвращает контактный телефон. */
    public String getPhone() {
        return phone;
    }

    /** Возвращает контактный адрес. */
    public String getAddress() {
        return address;
    }

    /** Возвращает комментарий к заказу. */
    public String getComment() {
        return comment;
    }

    /** Возвращает неизменяемую копию списка позиций. */
    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    /** Возвращает точную сумму сохранённых позиций через BigDecimal. */
    public BigDecimal getTotal() {
        return lines.stream()
                .map(OrderLine::getSubtotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
}
