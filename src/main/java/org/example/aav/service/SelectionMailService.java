package org.example.aav.service;

import java.util.List;
import java.util.UUID;
import org.example.aav.model.SelectedProduct;
import org.example.aav.model.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Этап 5: готовим письмо с товарами и сохраняем его в очереди для последующей отправки. Учебная последовательность. */
@Service
public class SelectionMailService {
  // Помощник Spring для выполнения SQL-запросов.
  private final JdbcTemplate jdbc;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param jdbc помощник Spring для выполнения SQL-запросов
   */
  public SelectionMailService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  /**
   * Собирает список товаров и сумму, затем сохраняет письмо в очереди базы данных.
   * @param user учётная запись пользователя
   * @param products список выбранных товаров для письма
   */
  public void enqueue(UserAccount user, List<SelectedProduct> products) {
    if (user.getEmail() == null || user.getEmail().isBlank()) return;
    // Текст письма.
    StringBuilder body = new StringBuilder("Ваш заказ — актуальный список выбранных продуктов:\n\n");
    if (products.isEmpty()) body.append("Список пуст. Все продукты удалены.\n");
    // Сумма цен всех выбранных товаров.
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
    // Очередной выбранный товар.
    for (var item : products) {
      // Товар, с которым выполняется действие.
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
