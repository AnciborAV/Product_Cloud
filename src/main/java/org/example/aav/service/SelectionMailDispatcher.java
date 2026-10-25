package org.example.aav.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Этап 5: обрабатываем очередь писем по расписанию, чтобы повторять отправку после ошибок. Учебная последовательность. */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SelectionMailDispatcher {
  // Средство записи сообщений о работе приложения.
  private static final Logger log = LoggerFactory.getLogger(SelectionMailDispatcher.class);
  // Помощник Spring для выполнения SQL-запросов.
  private final JdbcTemplate jdbc;
  // Средство отправки писем через почтовый сервер.
  private final JavaMailSender sender;
  // Адрес отправителя писем.
  private final String from;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param jdbc помощник Spring для выполнения SQL-запросов
   * @param sender средство отправки писем через почтовый сервер
   * @param from адрес отправителя писем
   */
  public SelectionMailDispatcher(JdbcTemplate jdbc, JavaMailSender sender,
      @Value("${app.mail.from}") String from) {
    if (from.isBlank()) throw new IllegalArgumentException("Укажите MAIL_FROM");
    this.jdbc = jdbc;
    this.sender = sender;
    this.from = from;
  }

  /**
   * Берёт до 20 писем из очереди, отправляет их и сохраняет результат попытки.
   */
  @Scheduled(fixedDelayString = "${app.mail.poll-delay:30000}")
  @Transactional
  public void dispatch() {
    // Строки писем, ожидающих отправки.
    var rows = jdbc.queryForList("SELECT id, recipient, body, subject FROM selection_mail_outbox "
        + "WHERE sent_at IS NULL AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP) ORDER BY CASE WHEN kind='PASSWORD_CHANGE' THEN 0 ELSE 1 END, created_at, id LIMIT 20 FOR UPDATE");
    // Одна строка результата sql-запроса.
    for (var row : rows) {
      // Уникальный номер записи.
      var id = (UUID) row.get("id");
      try {
        // Письмо для отправки через почтовый сервер.
        var message = sender.createMimeMessage();
        // Помощник заполнения адресов и содержимого письма.
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(new jakarta.mail.internet.InternetAddress(from, "Зелёная лавка", "UTF-8"));
        helper.setTo((String) row.get("recipient"));
        helper.setSubject((String) row.get("subject"));
        // Текст письма.
        String body = (String) row.get("body");
        helper.setText(BrandedMail.plain(body), BrandedMail.html((String) row.get("subject"), body));
        sender.send(message);
        jdbc.update("UPDATE selection_mail_outbox SET sent_at=CURRENT_TIMESTAMP, attempts=attempts+1 WHERE id=?", id);
      // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
      } catch (org.springframework.mail.MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
        jdbc.update("UPDATE selection_mail_outbox SET attempts=attempts+1 WHERE id=?", id);
        log.warn("Письмо {} не отправлено ({}), повторим позже", id, ex.getClass().getSimpleName());
        break; // После ошибки останавливаемся, чтобы сохранить порядок отправки писем.
      }
    }
  }
}
