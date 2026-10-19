package ru.aav.order;

import jakarta.persistence.*;
import ru.aav.account.CustomerAccount;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

/** Заказ хранит состав, цены и контакты на момент оформления. Оплаты в этом приложении нет. */
@Entity
@Table(name = "customer_orders", indexes = @Index(columnList = "account_id,created_at"))
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false)
    private CustomerAccount account;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(nullable = false, length = 80) private String recipient;
    @Column(nullable = false, length = 40) private String phone;
    @Column(nullable = false, length = 500) private String address;
    @Column(nullable = false, length = 1000) private String comment;
    @Column(nullable = false, length = 36, unique = true) private String checkoutToken;
    @ElementCollection
    @CollectionTable(name = "customer_order_lines", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_number")
    private List<OrderLine> lines = new ArrayList<>();
    protected CustomerOrder() { }
    public CustomerOrder(CustomerAccount account, List<OrderLine> lines, String recipient, String phone,
                         String address, String comment, String checkoutToken) {
        this.account = account; this.lines = new ArrayList<>(lines); this.createdAt = Instant.now();
        this.recipient = recipient.strip(); this.phone = phone.strip(); this.address = address.strip();
        this.comment = comment.strip(); this.checkoutToken = checkoutToken;
    }
    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getRecipient() { return recipient; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getComment() { return comment; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
    public BigDecimal getTotal() { return lines.stream().map(OrderLine::getSubtotal).reduce(new BigDecimal("0.00"), BigDecimal::add); }
}
