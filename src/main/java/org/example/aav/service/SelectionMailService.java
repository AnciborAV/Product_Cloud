package org.example.aav.service;

import java.util.List;
import java.util.UUID;
import org.example.aav.model.SelectedProduct;
import org.example.aav.model.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SelectionMailService {
  private final JdbcTemplate jdbc;

  public SelectionMailService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public void enqueue(UserAccount user, List<SelectedProduct> products) {
    if (user.getEmail() == null || user.getEmail().isBlank()) return;
    StringBuilder body = new StringBuilder("Ваш заказ — актуальный список выбранных продуктов:\n\n");
    if (products.isEmpty()) body.append("Список пуст. Все продукты удалены.\n");
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
    for (var item : products) {
      var product = item.getProduct().toProduct();
      total = total.add(product.price());
      body.append("• ").append(item.getProductName()).append(" — ")
          .append(product.unit()).append(" — ").append(product.price()).append(" ₽\n");
    }
    body.append("\nИтого: ").append(total).append(" ₽\nПо одной указанной упаковке каждого товара.\n");
    jdbc.update("INSERT INTO selection_mail_outbox(id,user_id,recipient,body) VALUES (?,?,?,?)",
        UUID.randomUUID(), user.getId(), user.getEmail(), body.toString());
  }
}
